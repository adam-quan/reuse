package com.example.platform.api;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

/**
 * Tiny connection bootstrap shared by the demo programs. {@code TEMPORAL_ADDRESS} overrides the
 * default {@code localhost:7233}. (For Temporal Cloud you would add TLS / API key here, or use the
 * SDK's environment-config profiles.)
 */
public final class TemporalConnection {
  private TemporalConnection() {}

  private static WorkflowServiceStubs stubs;

  public static synchronized WorkflowServiceStubs stubs() {
    if (stubs == null) {
      String target = System.getenv().getOrDefault("TEMPORAL_ADDRESS", "localhost:7233");
      stubs =
          WorkflowServiceStubs.newServiceStubs(
              WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }
    return stubs;
  }

  public static WorkflowClient workflowClient(String namespace) {
    return WorkflowClient.newInstance(
        stubs(), WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
  }

  public static ActivityClient activityClient(String namespace) {
    return ActivityClient.newInstance(
        stubs(), ActivityClientOptions.newBuilder().setNamespace(namespace).build());
  }
}
