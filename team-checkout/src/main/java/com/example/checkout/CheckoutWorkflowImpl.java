package com.example.checkout;

import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import com.example.platform.library.PlatformLibrary;
import java.util.Map;

/**
 * TEAM CHECKOUT - consumes the platform as an EMBEDDED LIBRARY (mode 1).
 *
 * <p>Depends on {@code platform-library}, registers the platform's activities and payment
 * workflow on ITS OWN worker, and runs the payment saga as a CHILD WORKFLOW in ITS OWN namespace.
 */
public class CheckoutWorkflowImpl implements CheckoutWorkflow {

  @Override
  public String checkout(Cart cart) {
    PaymentRequest req =
        new PaymentRequest(cart.cartId(), cart.customerId(), cart.totalCents(), "USD", cart.cardToken());

    // Reusable CHILD WORKFLOW from the platform library.
    PaymentResult pay = PlatformLibrary.paymentChild(req).process(req);

    // Reusable ACTIVITY from the platform library, executed in-process.
    PlatformLibrary.notifications()
        .sendEmail(
            new EmailRequest(
                "checkout-receipt:" + cart.cartId(),
                cart.email(),
                "checkout-" + pay.status().name().toLowerCase(),
                Map.of("cartId", cart.cartId())));

    return "cart " + cart.cartId() + ": payment " + pay.status();
  }
}
