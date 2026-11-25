package com.example.platform.library;

import io.temporal.common.SimplePlugin;
import io.temporal.worker.Worker;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The platform's reusable activities and workflows, packaged as a Temporal {@link SimplePlugin}.
 *
 * <p>One object carries everything a worker needs to host platform code:
 *
 * <ul>
 *   <li><b>registrations</b>: {@code PaymentWorkflowImpl}, {@code NotificationActivitiesImpl},
 *       {@code PaymentActivitiesImpl}
 *   <li><b>cross-cutting concerns</b>: {@link PlatformAuditInterceptor} audits every platform
 *       activity execution, whoever invoked it. A static {@code register(worker)} helper can't
 *       install factory-level interceptors; a plugin can. The same mechanism carries data
 *       converters / encryption codecs, context propagators and client interceptors.
 * </ul>
 *
 * <p>Attach it through options instead of calling registration code:
 *
 * <pre>{@code
 * WorkerFactory factory = WorkerFactory.newInstance(client,
 *     WorkerFactoryOptions.newBuilder()
 *         .setPlugins(new PlatformPlugin(config, "my-task-queue"))
 *         .build());
 * }</pre>
 *
 * <p>Used by the platform's own worker, by teams embedding the library (Checkout), and by the
 * library's tests, so registrations and interceptors never drift between them.
 *
 * <p>Note: {@code SimplePlugin} is {@code @Experimental} in the Java SDK.
 */
public class PlatformPlugin extends SimplePlugin {
  public static final String NAME = "com.example.platform";

  /** Dependencies the implementations need (provider clients, credentials, ...). */
  public record Config(String emailProvider, String paymentGateway) {}

  private final Config config;
  @Nullable private final String taskQueue;

  /** Registers the platform library on every worker created by the factory. */
  public PlatformPlugin(Config config) {
    this(config, null);
  }

  /**
   * Registers the platform library only on the worker for {@code taskQueue}. Use this when the
   * factory hosts several task queues and only one of them should run platform code.
   */
  public PlatformPlugin(Config config, @Nullable String taskQueue) {
    // Builder-configured parts: applied by SimplePlugin to the factory / clients.
    super(SimplePlugin.newBuilder(NAME).addWorkerInterceptors(new PlatformAuditInterceptor()));
    this.config = config;
    this.taskQueue = taskQueue;
  }

  @Override
  public void initializeWorker(@Nonnull String taskQueue, @Nonnull Worker worker) {
    super.initializeWorker(taskQueue, worker);
    if (this.taskQueue != null && !this.taskQueue.equals(taskQueue)) {
      return;
    }
    worker.registerWorkflowImplementationTypes(PaymentWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new NotificationActivitiesImpl(config.emailProvider()),
        new PaymentActivitiesImpl(config.paymentGateway()));
  }
}
