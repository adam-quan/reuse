package com.example.platform.library;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.platform.api.PaymentWorkflow;
import com.example.platform.api.model.PaymentRequest;
import com.example.platform.api.model.PaymentResult;
import com.example.platform.api.model.PaymentStatus;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.worker.WorkerFactoryOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The platform team ships the library with tests that run the real implementations through the
 * same {@link PlatformPlugin} used in production - consumers inherit that confidence.
 */
class PaymentWorkflowTest {
  private static final String TQ = "test";
  private TestWorkflowEnvironment env;

  @BeforeEach
  void setUp() {
    // Same plugin as production: registrations + audit interceptor.
    env =
        TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder()
                .setWorkerFactoryOptions(
                    WorkerFactoryOptions.newBuilder()
                        .setPlugins(
                            new PlatformPlugin(new PlatformPlugin.Config("test-email", "test-gateway")))
                        .build())
                .build());
    env.newWorker(TQ);
    env.start();
  }

  @AfterEach
  void tearDown() {
    env.close();
  }

  @ParameterizedTest
  @CsvSource({"tok_visa,CAPTURED", "tok_declined,DECLINED", "tok_capture_fails,VOIDED"})
  void paymentSaga(String card, PaymentStatus expected) {
    PaymentWorkflow wf =
        env.getWorkflowClient()
            .newWorkflowStub(
                PaymentWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TQ).setWorkflowId("pay-" + card).build());

    PaymentResult result = wf.process(new PaymentRequest("ORD-1", "cust-1", 1_000, "USD", card));

    assertEquals(expected, result.status(), () -> "reason: " + result.failureReason());
  }
}
