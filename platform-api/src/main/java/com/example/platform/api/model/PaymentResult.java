package com.example.platform.api.model;

public record PaymentResult(
    String orderId, PaymentStatus status, String authId, String captureId, String failureReason) {

  public static PaymentResult declined(String orderId, String reason) {
    return new PaymentResult(orderId, PaymentStatus.DECLINED, null, null, reason);
  }

  public static PaymentResult voided(String orderId, String authId, String reason) {
    return new PaymentResult(orderId, PaymentStatus.VOIDED, authId, null, reason);
  }

  public static PaymentResult captured(String orderId, String authId, String captureId) {
    return new PaymentResult(orderId, PaymentStatus.CAPTURED, authId, captureId, null);
  }
}
