package com.example.orders;

import com.example.platform.api.PlatformClients;
import com.example.platform.api.PlatformService;
import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import java.util.Map;

/**
 * TEAM ORDERS - consumes the platform as a SERVICE over NEXUS (mode 3).
 *
 * <p>Depends on {@code platform-api} only. No platform implementation code is compiled into,
 * registered on, or scaled with the orders worker. It runs in its own namespace and reaches the
 * platform namespace through the {@code platform-services-endpoint} Nexus endpoint.
 */
public class OrderWorkflowImpl implements OrderWorkflow {

  // Typed Nexus stub: compile-time checked against the platform's service contract.
  private final PlatformService platform = PlatformClients.nexus();

  @Override
  public OrderResult placeOrder(Order o) {
    // Reusable WORKFLOW, run in the platform namespace via Nexus.
    PaymentResult pay =
        platform.processPayment(
            new PaymentRequest(o.orderId(), o.customerId(), o.totalCents(), "USD", o.cardToken()));

    // Reusable ACTIVITY, run as a STANDALONE ACTIVITY in the platform namespace via Nexus.
    // Its result is delivered back to this workflow via a Nexus completion callback.
    EmailResult email =
        platform.sendEmail(
            new EmailRequest(
                "order-" + pay.status() + ":" + o.orderId(),
                o.email(),
                "order-" + pay.status().name().toLowerCase(),
                Map.of("orderId", o.orderId(), "simulateOutage", "true")));

    return new OrderResult(o.orderId(), pay.status(), pay.failureReason(), email.messageId());
  }
}
