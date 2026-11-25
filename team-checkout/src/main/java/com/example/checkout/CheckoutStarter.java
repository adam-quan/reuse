package com.example.checkout;

import com.example.platform.api.TemporalConnection;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.LoggerFactory;

public class CheckoutStarter {
  public static void main(String[] args) {
    WorkflowClient client = TemporalConnection.workflowClient(CheckoutWorkflow.NAMESPACE);
    Cart cart =
        new Cart("CART-" + System.currentTimeMillis() / 1000, "cust-7", "shopper@example.com", 4_999, "tok_visa");

    CheckoutWorkflow wf =
        client.newWorkflowStub(
            CheckoutWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("checkout:" + cart.cartId())
                .setTaskQueue(CheckoutWorkflow.TASK_QUEUE)
                .build());

    String result = wf.checkout(cart);
    LoggerFactory.getLogger(CheckoutStarter.class)
        .info("[library mode] {}  (namespace '{}')", result, CheckoutWorkflow.NAMESPACE);
    System.exit(0);
  }
}
