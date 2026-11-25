package com.example.platform.api;

import io.temporal.common.RetryOptions;
import java.time.Duration;

/**
 * Where the platform's reusable services live, and the platform team's recommended defaults.
 *
 * <p>Everything in the {@code platform-api} module is the PUBLIC CONTRACT. A team consuming the
 * platform "as a service" (Nexus or Standalone Activities) depends on this module only: it
 * contains no implementation code, so the platform team can ship fixes and new versions without
 * every consumer re-deploying.
 */
public final class Platform {
  private Platform() {}

  /** Namespace that hosts the platform team's workers. */
  public static final String NAMESPACE = "platform";

  /** Task queue polled by the platform worker (activities, workflows and Nexus tasks). */
  public static final String TASK_QUEUE = "platform-services";

  /**
   * Nexus endpoint registered in the cluster (see scripts/setup.sh). Callers address the
   * endpoint, never the platform namespace or task queue directly.
   */
  public static final String NEXUS_ENDPOINT = "platform-services-endpoint";

  /** Name of the Nexus service exposed on the endpoint (see {@link PlatformService}). */
  public static final String NEXUS_SERVICE = "platform.v1";

  /** Retry policy shared by every invocation mode of the platform's activities. */
  public static RetryOptions defaultRetryOptions() {
    return RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofSeconds(1))
        .setBackoffCoefficient(2)
        .setMaximumInterval(Duration.ofSeconds(30))
        .setMaximumAttempts(10)
        .build();
  }

  /** Activity ID for an email. Same key => same activity => no duplicate sends. */
  public static String emailActivityId(String idempotencyKey) {
    return "email:" + idempotencyKey;
  }

  /** Workflow ID for a payment. Same order => same workflow => never charged twice. */
  public static String paymentWorkflowId(String orderId) {
    return "payment:" + orderId;
  }
}
