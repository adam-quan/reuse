package com.example.platform.library;

import com.example.platform.api.NotificationActivities;
import com.example.platform.api.PaymentWorkflow;
import com.example.platform.api.Platform;
import com.example.platform.api.model.PaymentRequest;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Workflow-side stub factories for teams that EMBED the platform library.
 *
 * <p>A team that wants to run platform activities / child workflows inside its own namespace and
 * on its own workers depends on {@code platform-library}, adds {@link PlatformPlugin} to its
 * worker factory, and uses the stubs below from its workflows.
 *
 * <p>Trade-off vs. service mode: no extra hop and full control, but every consuming team now
 * deploys (and must upgrade) platform code, holds the platform's credentials, and scales it.
 */
public final class PlatformLibrary {
  private PlatformLibrary() {}

  /** Platform notification activities, executed in the CALLER's namespace and task queue. */
  public static NotificationActivities notifications() {
    return Workflow.newActivityStub(
        NotificationActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(Platform.defaultRetryOptions())
            .build());
  }

  /** Platform payment saga as a CHILD WORKFLOW in the caller's namespace. */
  public static PaymentWorkflow paymentChild(PaymentRequest forRequest) {
    return Workflow.newChildWorkflowStub(
        PaymentWorkflow.class,
        ChildWorkflowOptions.newBuilder()
            .setWorkflowId(Platform.paymentWorkflowId(forRequest.orderId()))
            .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
            .build());
  }
}
