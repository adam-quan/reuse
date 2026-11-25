package com.example.orders;

import com.example.platform.api.TemporalConnection;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.LoggerFactory;

/** Team Orders' worker. Registers ONLY its own workflow - platform capabilities are remote. */
public class OrdersWorker {
  public static void main(String[] args) {
    WorkerFactory factory =
        WorkerFactory.newInstance(TemporalConnection.workflowClient(OrderWorkflow.NAMESPACE));
    Worker worker = factory.newWorker(OrderWorkflow.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
    factory.start();
    LoggerFactory.getLogger(OrdersWorker.class)
        .info(
            "orders worker started: namespace={} taskQueue={} (platform consumed via Nexus)",
            OrderWorkflow.NAMESPACE,
            OrderWorkflow.TASK_QUEUE);
  }
}
