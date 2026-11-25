package com.example.platform.api;

import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Reusable payment saga: fraud check -> authorize -> capture, voiding the authorization if
 * capture fails.
 *
 * <p>Runs either as a CHILD WORKFLOW in a consumer's namespace (library mode) or in the platform
 * namespace behind the {@code process-payment} Nexus operation (service mode).
 */
@WorkflowInterface
public interface PaymentWorkflow {

  @WorkflowMethod(name = "payments.ProcessPayment.v1")
  PaymentResult process(PaymentRequest request);
}
