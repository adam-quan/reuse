package com.example.platform.service;

import com.example.platform.api.NotificationActivities;
import com.example.platform.api.PaymentWorkflow;
import com.example.platform.api.Platform;
import com.example.platform.api.PlatformService;
import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import io.nexusrpc.handler.HandlerException;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperation;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.nexus.TemporalOperationStartContext;
import java.time.Duration;

/**
 * Exposes the platform library as the {@code platform.v1} Nexus service.
 *
 * <p>This is the "reusable library -> reusable service" step: the same activities and workflows
 * from {@code platform-library} are now invoked across namespace boundaries by teams that depend
 * on {@code platform-api} only.
 */
@ServiceImpl(service = PlatformService.class)
public class PlatformServiceImpl {

  /**
   * {@code send-email}: a Nexus operation backed by a STANDALONE ACTIVITY.
   *
   * <p>The handler starts the activity in the platform namespace and returns an operation token
   * right away. When the activity completes, the Temporal server delivers the result to the
   * caller's workflow through a Nexus completion callback - no wrapper workflow is needed for a
   * single-step job.
   */
  @TemporalOperation
  public TemporalOperationResult<EmailResult> sendEmail(
      TemporalOperationStartContext ctx, TemporalNexusClient client, EmailRequest req) {
    if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
      throw new HandlerException(
          HandlerException.ErrorType.BAD_REQUEST,
          new IllegalArgumentException("idempotencyKey is required"));
    }
    return client.startActivity(
        NotificationActivities.class,
        NotificationActivities::sendEmail,
        req,
        StartActivityOptions.newBuilder()
            .setId(Platform.emailActivityId(req.idempotencyKey()))
            .setTaskQueue(Platform.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setScheduleToCloseTimeout(Duration.ofMinutes(10))
            .setRetryOptions(Platform.defaultRetryOptions())
            .setSummary("email " + req.template() + " -> " + req.to() + " (via Nexus)")
            .build());
  }

  /** {@code process-payment}: a Nexus operation backed by the reusable payment WORKFLOW. */
  @TemporalOperation
  public TemporalOperationResult<PaymentResult> processPayment(
      TemporalOperationStartContext ctx, TemporalNexusClient client, PaymentRequest req) {
    if (req.orderId() == null || req.orderId().isBlank()) {
      throw new HandlerException(
          HandlerException.ErrorType.BAD_REQUEST,
          new IllegalArgumentException("orderId is required"));
    }
    return client.startWorkflow(
        PaymentWorkflow.class,
        PaymentWorkflow::process,
        req,
        WorkflowOptions.newBuilder()
            // Deterministic business ID: an order can't be charged twice, even if the caller or
            // the Nexus request is retried.
            .setWorkflowId(Platform.paymentWorkflowId(req.orderId()))
            .setTaskQueue(Platform.TASK_QUEUE)
            .build());
  }
}
