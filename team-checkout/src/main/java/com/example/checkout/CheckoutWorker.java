package com.example.checkout;

import com.example.platform.api.TemporalConnection;
import com.example.platform.library.PlatformPlugin;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import org.slf4j.LoggerFactory;

/**
 * Team Checkout's worker. It embeds the platform library by adding {@link PlatformPlugin}: one
 * line brings the platform's activities, payment workflow AND its audit interceptor. In library
 * mode the consuming team runs (and supplies credentials for) platform code.
 */
public class CheckoutWorker {
  public static void main(String[] args) {
    PlatformPlugin platform =
        new PlatformPlugin(
            new PlatformPlugin.Config("checkout-ses", "checkout-adyen"), CheckoutWorkflow.TASK_QUEUE);

    WorkerFactory factory =
        WorkerFactory.newInstance(
            TemporalConnection.workflowClient(CheckoutWorkflow.NAMESPACE),
            WorkerFactoryOptions.newBuilder().setPlugins(platform).build());

    Worker worker = factory.newWorker(CheckoutWorkflow.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CheckoutWorkflowImpl.class); // Checkout's own code

    factory.start();
    LoggerFactory.getLogger(CheckoutWorker.class)
        .info(
            "checkout worker started: namespace={} taskQueue={} plugins=[{}]",
            CheckoutWorkflow.NAMESPACE,
            CheckoutWorkflow.TASK_QUEUE,
            platform.getName());
  }
}
