package com.example.platform.library;

import com.example.platform.api.PaymentActivities;
import com.example.platform.api.PaymentWorkflow;
import com.example.platform.api.Platform;
import com.example.platform.api.model.Authorization;
import com.example.platform.api.model.Capture;
import com.example.platform.api.model.FraudCheckResult;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * Reusable payment saga: fraud check -> authorize -> capture, with compensations managed by
 * {@link Saga}. The activity stub routes to whatever task queue this workflow runs on, so the same
 * code works embedded in a consumer's worker or hosted by the platform worker.
 *
 * <p>Outcomes:
 *
 * <ul>
 *   <li>fraud rejected -> {@code DECLINED} (nothing to compensate yet)
 *   <li>a step fails after authorization -> compensations run -> {@code VOIDED}
 *   <li>workflow cancelled mid-flight (e.g. the Nexus caller cancels {@code process-payment}, or a
 *       parent workflow is cancelled) -> compensations run -> workflow ends CANCELLED
 * </ul>
 */
public class PaymentWorkflowImpl implements PaymentWorkflow {
  private static final Logger log = Workflow.getLogger(PaymentWorkflowImpl.class);

  private final PaymentActivities payments =
      Workflow.newActivityStub(
          PaymentActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .setRetryOptions(Platform.defaultRetryOptions())
              .build());

  @Override
  public PaymentResult process(PaymentRequest req) {
    FraudCheckResult fraud = payments.fraudCheck(req);
    if (!fraud.approved()) {
      return PaymentResult.declined(req.orderId(), fraud.reason());
    }

    // Compensations run in reverse registration order, one at a time.
    Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
    Authorization auth = null;
    try {
      auth = payments.authorizeCard(req);
      // Register the undo only once the step it undoes has succeeded.
      saga.addCompensation(payments::voidAuthorization, auth);

      Capture capture = payments.captureCharge(req, auth);
      return PaymentResult.captured(req.orderId(), auth.authId(), capture.captureId());

    } catch (ActivityFailure | CanceledFailure e) {
      log.warn("payment {} did not complete ({}), compensating", req.orderId(), reason(e));
      // Detached scope: compensations must still execute while this workflow is being cancelled.
      Workflow.newDetachedCancellationScope(saga::compensate).run();

      CanceledFailure cancellation = asCancellation(e);
      if (cancellation != null) {
        throw cancellation; // cleaned up, and the workflow still ends as CANCELLED
      }
      if (auth == null) {
        throw e; // authorize itself failed: nothing was held, surface the failure
      }
      return PaymentResult.voided(req.orderId(), auth.authId(), reason(e));
    }
  }

  /** Cancellation can surface directly or as the cause of the in-flight activity's failure. */
  private static CanceledFailure asCancellation(RuntimeException e) {
    if (e instanceof CanceledFailure c) return c;
    return e.getCause() instanceof CanceledFailure c ? c : null;
  }

  private static String reason(RuntimeException e) {
    return e.getCause() instanceof ApplicationFailure app ? app.getOriginalMessage() : e.getMessage();
  }
}
