package com.example.orders;

public record Order(String orderId, String customerId, String email, long totalCents, String cardToken) {}
