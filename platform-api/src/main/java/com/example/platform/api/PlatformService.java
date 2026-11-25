package com.example.platform.api;

import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/**
 * The platform's Nexus service contract: what other namespaces can call, with typed inputs and
 * outputs. Implemented by {@code PlatformServiceImpl} in {@code platform-service}.
 *
 * <p>Callers don't know (or care) how each operation is implemented:
 *
 * <ul>
 *   <li>{@code send-email} is backed by a STANDALONE ACTIVITY
 *   <li>{@code process-payment} is backed by a WORKFLOW
 * </ul>
 */
@Service(name = Platform.NEXUS_SERVICE)
public interface PlatformService {

  @Operation(name = "send-email")
  EmailResult sendEmail(EmailRequest request);

  @Operation(name = "process-payment")
  PaymentResult processPayment(PaymentRequest request);
}
