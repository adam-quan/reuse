package com.example.platform.library;

import com.example.platform.api.NotificationActivities;
import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reusable email activity. The exact same code runs:
 *
 * <ul>
 *   <li>in-process inside another team's workflow (library mode),
 *   <li>as a Standalone Activity started directly from a client, and
 *   <li>behind a Nexus operation.
 * </ul>
 *
 * Nothing in here knows or cares which mode invoked it.
 */
public class NotificationActivitiesImpl implements NotificationActivities {
  private static final Logger log = LoggerFactory.getLogger(NotificationActivitiesImpl.class);

  private final String provider; // in real life: an email provider client + credentials

  public NotificationActivitiesImpl(String provider) {
    this.provider = provider;
  }

  @Override
  public EmailResult sendEmail(EmailRequest req) {
    ActivityInfo info = Activity.getExecutionContext().getInfo();

    if (req.to() == null || req.to().isBlank() || req.template() == null) {
      // Bad input will never succeed: don't burn retries on it.
      throw ApplicationFailure.newNonRetryableFailure(
          "'to' and 'template' are required", "InvalidEmailRequest");
    }

    // Simulate a flaky provider: the first attempt fails, Temporal retries it transparently.
    if (info.getAttempt() == 1 && "true".equals(req.vars().get("simulateOutage"))) {
      log.warn(
          "⚠️  [DEMO] simulated email-provider outage for {} on attempt 1 (simulateOutage=true)"
              + " - Temporal will retry automatically",
          req.to());
      throw new RuntimeException(provider + ": 503 service unavailable");
    }

    // Provider-side idempotency: message id derived from the idempotency key.
    String messageId =
        "msg_"
            + UUID.nameUUIDFromBytes(req.idempotencyKey().getBytes(StandardCharsets.UTF_8))
                .toString()
                .substring(0, 8);
    // How it was invoked (standalone / workflow) is logged by PlatformAuditInterceptor.
    log.info("📧 sent '{}' to {} id={} provider={}", req.template(), req.to(), messageId, provider);
    return new EmailResult(messageId, provider);
  }
}
