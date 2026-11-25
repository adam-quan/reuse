package com.example.checkout;

public record Cart(String cartId, String customerId, String email, long totalCents, String cardToken) {}
