package com.example.orders;

import com.example.platform.api.model.PaymentStatus;

public record OrderResult(String orderId, PaymentStatus payment, String reason, String emailMessageId) {}
