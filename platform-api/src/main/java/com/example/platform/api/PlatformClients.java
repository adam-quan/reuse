package com.example.platform.api;

import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import io.temporal.api.enums.v1.ActivityIdConflictPolicy;
import io.temporal.api.enums.v1.ActivityIdReusePolicy;
import io.temporal.client.ActivityAlreadyStartedException;
import io.temporal.client.ActivityClient;
import io.temporal.client.StartActivityOptions;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Caller-side helpers that bake in the platform team's recommended timeouts and retry policies, so
 * every consuming team gets them for free and never hard-codes a namespace or task queue.
 */
public final class PlatformClients {
  private PlatformClients() {}

  // =========================================================================================
  // From a WORKFLOW in any namespace -> Nexus
  // =========================================================================================

  /**
   * Typed Nexus stub for use inside a caller's workflow.
   *
   * <pre>{@code
   * PlatformService platform = PlatformClients.nexus();
   * PaymentResult r = platform.processPayment(req);   // durable, cross-namespace
   * }</pre>
   */
  public static PlatformService nexus() {
    return Workflow.newNexusServiceStub(
        PlatformService.class,
        NexusServiceOptions.newBuilder()
            .setEndpoint(Platform.NEXUS_ENDPOINT)
            .setOperationOptions(
                NexusOperationOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofMinutes(30))
                    .build())
            .build());
  }

  // =========================================================================================
  // From PLAIN APPLICATION CODE (no workflow) -> Standalone Activities
  // =========================================================================================

  /**
   * Durably enqueue a SendEmail activity straight from a client - no workflow needed. Think
   * "durable job queue with retries, timeouts and visibility".
   *
   * <p>The idempotency key becomes the Activity ID: if the activity is still running we attach to
   * it, and if it already completed we return the original result instead of sending again.
   *
   * @param activityClient an {@link ActivityClient} connected to {@link Platform#NAMESPACE}
   */
  public static EmailResult sendEmailStandalone(ActivityClient activityClient, EmailRequest req) {
    String id = Platform.emailActivityId(req.idempotencyKey());
    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId(id)
            .setTaskQueue(Platform.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setScheduleToCloseTimeout(Duration.ofMinutes(10))
            .setRetryOptions(Platform.defaultRetryOptions())
            .setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
            .setIdReusePolicy(ActivityIdReusePolicy.ACTIVITY_ID_REUSE_POLICY_REJECT_DUPLICATE)
            .setSummary("email " + req.template() + " -> " + req.to())
            .build();
    try {
      return activityClient.execute(
          NotificationActivities.class, NotificationActivities::sendEmail, options, req);
    } catch (ActivityAlreadyStartedException alreadyDone) {
      // Sent earlier with this key: return that outcome rather than sending twice.
      return activityClient.getHandle(id, null, EmailResult.class).getResult();
    }
  }
}
