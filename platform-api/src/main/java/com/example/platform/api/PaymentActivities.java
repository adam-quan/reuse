package com.example.platform.api;

import com.example.platform.api.model.Authorization;
import com.example.platform.api.model.Capture;
import com.example.platform.api.model.FraudCheckResult;
import com.example.platform.api.model.PaymentRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** The individual steps of the payment saga (orchestrated by {@link PaymentWorkflow}). */
@ActivityInterface
public interface PaymentActivities {

  @ActivityMethod(name = "payments.FraudCheck.v1")
  FraudCheckResult fraudCheck(PaymentRequest request);

  @ActivityMethod(name = "payments.AuthorizeCard.v1")
  Authorization authorizeCard(PaymentRequest request);

  @ActivityMethod(name = "payments.CaptureCharge.v1")
  Capture captureCharge(PaymentRequest request, Authorization auth);

  @ActivityMethod(name = "payments.VoidAuthorization.v1")
  void voidAuthorization(Authorization auth);
}
