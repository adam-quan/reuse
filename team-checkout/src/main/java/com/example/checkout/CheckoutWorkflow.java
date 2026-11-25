package com.example.checkout;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CheckoutWorkflow {
  String NAMESPACE = "checkout";
  String TASK_QUEUE = "checkout";

  @WorkflowMethod
  String checkout(Cart cart);
}
