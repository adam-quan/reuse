package com.example.orders;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
  String NAMESPACE = "orders";
  String TASK_QUEUE = "orders";

  @WorkflowMethod
  OrderResult placeOrder(Order order);
}
