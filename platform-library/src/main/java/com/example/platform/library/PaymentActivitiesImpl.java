package com.example.platform.library;

import com.example.platform.api.PaymentActivities;
import com.example.platform.api.model.Authorization;
import com.example.platform.api.model.Capture;
import com.example.platform.api.model.FraudCheckResult;
import com.example.platform.api.model.PaymentRequest;
import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simulated payment gateway. */
public class PaymentActivitiesImpl implements PaymentActivities {
  private static final Logger log = LoggerFactory.getLogger(PaymentActivitiesImpl.class);

  /** Demo knob: fraud check rejects this card. */
  public static final String CARD_DECLINED = "tok_declined";

  /** Demo knob: capture fails for this card, exercising the saga's compensation. */
  public static final String CARD_CAPTURE_FAILS = "tok_capture_fails";

  private final String gateway; // in real life: gateway client + API keys

  public PaymentActivitiesImpl(String gateway) {
    this.gateway = gateway;
  }

  @Override
  public FraudCheckResult fraudCheck(PaymentRequest req) {
    if (CARD_DECLINED.equals(req.cardToken()) || req.amountCents() > 1_000_000) {
      log.info("🛡️  fraud check REJECTED order={}", req.orderId());
      return new FraudCheckResult(false, "risk score too high");
    }
    log.info("🛡️  fraud check passed order={}", req.orderId());
    return new FraudCheckResult(true, null);
  }

  @Override
  public Authorization authorizeCard(PaymentRequest req) {
    Authorization auth = new Authorization("auth_" + req.orderId());
    log.info("💳 authorized {}¢ order={} authId={} via={}", req.amountCents(), req.orderId(), auth.authId(), gateway);
    return auth;
  }

  @Override
  public Capture captureCharge(PaymentRequest req, Authorization auth) {
    if (CARD_CAPTURE_FAILS.equals(req.cardToken())) {
      throw ApplicationFailure.newNonRetryableFailure(
          "issuer rejected capture for " + auth.authId(), "CaptureRejected");
    }
    Capture capture = new Capture("cap_" + req.orderId());
    log.info("💰 captured order={} captureId={}", req.orderId(), capture.captureId());
    return capture;
  }

  @Override
  public void voidAuthorization(Authorization auth) {
    log.info("↩️  voided authorization {} (compensation)", auth.authId());
  }
}
