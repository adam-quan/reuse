package com.example.orders;

import com.example.platform.api.TemporalConnection;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places three orders that exercise the happy path, a fraud decline, and the payment saga's
 * compensation - all executed by the platform via Nexus.
 */
public class OrdersStarter {
  private static final Logger log = LoggerFactory.getLogger(OrdersStarter.class);

  public static void main(String[] args) {
    WorkflowClient client = TemporalConnection.workflowClient(OrderWorkflow.NAMESPACE);
    long n = System.currentTimeMillis() / 1000;

    List<Order> orders =
        List.of(
            new Order("ORD-" + n + "-ok", "cust-42", "buyer@example.com", 12_500, "tok_visa"),
            new Order("ORD-" + n + "-fraud", "cust-42", "buyer@example.com", 9_900, "tok_declined"),
            new Order("ORD-" + n + "-void", "cust-42", "buyer@example.com", 7_000, "tok_capture_fails"));

    List<CompletableFuture<Void>> runs =
        orders.stream()
            .map(
                o -> {
                  OrderWorkflow wf =
                      client.newWorkflowStub(
                          OrderWorkflow.class,
                          WorkflowOptions.newBuilder()
                              .setWorkflowId("order:" + o.orderId())
                              .setTaskQueue(OrderWorkflow.TASK_QUEUE)
                              .build());
                  return WorkflowClient.execute(wf::placeOrder, o)
                      .thenAccept(
                          r ->
                              log.info(
                                  "[nexus mode] {}  payment={}  email={}  {}",
                                  r.orderId(),
                                  r.payment(),
                                  r.emailMessageId(),
                                  r.reason() == null ? "" : "(" + r.reason() + ")"));
                })
            .toList();

    CompletableFuture.allOf(runs.toArray(CompletableFuture[]::new)).join();
    System.exit(0);
  }
}
