package com.example.platform.api;

import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Reusable notification activities.
 *
 * <p>The interface is part of the contract; the implementation lives in {@code platform-library}.
 * Explicit, versioned type names decouple the wire contract from Java method names.
 */
@ActivityInterface
public interface NotificationActivities {

  @ActivityMethod(name = "notifications.SendEmail.v1")
  EmailResult sendEmail(EmailRequest request);
}
