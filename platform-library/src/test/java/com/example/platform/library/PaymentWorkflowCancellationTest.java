package com.example.platform.library;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.platform.api.PaymentActivities;
import com.example.platform.api.PaymentWorkflow;
import com.example.platform.api.model.Authorization;
import com.example.platform.api.model.Capture;
import com.example.platform.api.model.FraudCheckResult;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import io.temporal.activity.Activity;
import io.temporal.client.ActivityCanceledException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.CanceledFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Cancelling the payment workflow while capture is in flight (e.g. the Nexus caller cancels
 * {@code process-payment}) must still void the authorization - otherwise the customer's card keeps
 * a hold - and the workflow must end CANCELLED, not complete normally.
 *
 * <p>In the Java SDK, cancellation of an in-flight activity surfaces as an {@code ActivityFailure}
 * caused by {@code CanceledFailure}; the saga's compensation must run in a detached scope to
 * execute at all while the workflow is being cancelled.
 */
class PaymentWorkflowCancellationTest {
  private static final String TQ = "cancel-test";

  /** Capture blocks until cancelled; records whether the compensation ran. */
  static class BlockingCapturePayments implements PaymentActivities {
    final CountDownLatch captureStarted = new CountDownLatch(1);
    final CountDownLatch voided = new CountDownLatch(1);

    @Override
    public FraudCheckResult fraudCheck(PaymentRequest r) {
      return new FraudCheckResult(true, null);
    }

    @Override
    public Authorization authorizeCard(PaymentRequest r) {
      return new Authorization("auth_" + r.orderId());
    }

    @Override
    public Capture captureCharge(PaymentRequest r, Authorization auth) {
      captureStarted.countDown();
      long deadline = System.currentTimeMillis() + 2_000;
      try {
        while (System.currentTimeMillis() < deadline) {
          Activity.getExecutionContext().heartbeat(null); // delivers the cancellation
          Thread.sleep(50);
        }
      } catch (ActivityCanceledException | InterruptedException e) {
        // expected
      }
      throw new IllegalStateException("capture was not expected to finish");
    }

    @Override
    public void voidAuthorization(Authorization auth) {
      voided.countDown();
    }
  }

  private TestWorkflowEnvironment env;
  private BlockingCapturePayments payments;

  @BeforeEach
  void setUp() {
    env = TestWorkflowEnvironment.newInstance();
    Worker worker = env.newWorker(TQ);
    worker.registerWorkflowImplementationTypes(PaymentWorkflowImpl.class);
    payments = new BlockingCapturePayments();
    worker.registerActivitiesImplementations(payments);
    env.start();
  }

  @AfterEach
  void tearDown() {
    env.close();
  }

  @Test
  void cancellationDuringCaptureVoidsTheAuthorization() throws Exception {
    PaymentWorkflow wf =
        env.getWorkflowClient()
            .newWorkflowStub(
                PaymentWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TQ).setWorkflowId("pay-cancel").build());
    WorkflowClient.start(wf::process, new PaymentRequest("ORD-C", "cust", 1_000, "USD", "tok_visa"));

    assertTrue(payments.captureStarted.await(5, TimeUnit.SECONDS), "capture never started");
    WorkflowStub.fromTyped(wf).cancel();

    WorkflowFailedException result =
        assertThrows(
            WorkflowFailedException.class,
            () -> WorkflowStub.fromTyped(wf).getResult(PaymentResult.class));
    assertInstanceOf(CanceledFailure.class, result.getCause(), "workflow should end CANCELLED");
    assertTrue(payments.voided.await(5, TimeUnit.SECONDS), "authorization was not voided");
  }
}
