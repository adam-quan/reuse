package com.example.platform.api.model;

import java.util.Map;

/**
 * Ask the notification service to send one email.
 *
 * @param idempotencyKey business key for this send, e.g. {@code "order-confirmation:ORD-1001"}.
 *     It becomes the Activity ID, so the same key is never sent twice.
 */
public record EmailRequest(String idempotencyKey, String to, String template, Map<String, String> vars) {
  public EmailRequest {
    vars = vars == null ? Map.of() : Map.copyOf(vars);
  }
}
