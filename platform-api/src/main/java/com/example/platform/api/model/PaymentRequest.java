package com.example.platform.api.model;

/**
 * Ask the payment service to charge a card.
 *
 * @param orderId business key; it becomes the payment Workflow ID, so an order can never be
 *     charged twice.
 */
public record PaymentRequest(
    String orderId, String customerId, long amountCents, String currency, String cardToken) {}
