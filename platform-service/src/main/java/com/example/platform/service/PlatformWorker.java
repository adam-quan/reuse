package com.example.platform.service;

import com.example.platform.api.Platform;
import com.example.platform.api.TemporalConnection;
import com.example.platform.library.PlatformPlugin;
import io.temporal.common.SimplePlugin;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform worker - owned and operated by the Platform team.
 *
 * <p>One worker in the {@code platform} namespace, assembled entirely from plugins:
 *
 * <ul>
 *   <li>{@link PlatformPlugin}: the reusable activities + workflows + audit interceptor (the very
 *       same plugin a team embeds when it uses the library in-process)
 *   <li>a {@link SimplePlugin} built inline: the {@code platform.v1} Nexus service handler, which
 *       only the platform team runs
 * </ul>
 */
public class PlatformWorker {
  private static final Logger log = LoggerFactory.getLogger(PlatformWorker.class);

  public static void main(String[] args) {
    PlatformPlugin library =
        new PlatformPlugin(
            new PlatformPlugin.Config("platform-sendgrid", "platform-stripe"), Platform.TASK_QUEUE);

    SimplePlugin nexusService =
        SimplePlugin.newBuilder("com.example.platform.nexus")
            .registerNexusServiceImplementation(new PlatformServiceImpl())
            .build();

    WorkerFactory factory =
        WorkerFactory.newInstance(
            TemporalConnection.workflowClient(Platform.NAMESPACE),
            WorkerFactoryOptions.newBuilder().setPlugins(library, nexusService).build());
    factory.newWorker(Platform.TASK_QUEUE);
    factory.start();

    log.info(
        "platform worker started: namespace={} taskQueue={} plugins=[{}, {}]",
        Platform.NAMESPACE,
        Platform.TASK_QUEUE,
        library.getName(),
        nexusService.getName());
  }
}
