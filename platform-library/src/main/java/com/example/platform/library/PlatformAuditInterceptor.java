package com.example.platform.library;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-cutting concern shipped inside {@link PlatformPlugin}: audits every PLATFORM activity
 * execution (type, how it was invoked, attempt, duration, outcome) on whichever worker runs it -
 * the platform's own worker or a team's worker that embeds the library.
 *
 * <p>Swap the log line for metrics, tracing spans, or an audit-log write in a real system. The
 * activity implementations stay free of this plumbing.
 */
public class PlatformAuditInterceptor extends WorkerInterceptorBase {
  private static final Logger log = LoggerFactory.getLogger("platform-audit");

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return new ActivityInboundCallsInterceptorBase(next) {
      private ActivityInfo info;

      @Override
      public void init(ActivityExecutionContext context) {
        this.info = context.getInfo();
        super.init(context);
      }

      @Override
      public ActivityOutput execute(ActivityInput input) {
        if (!isPlatformActivity(info.getActivityType())) {
          return super.execute(input); // a team's own activity on the same factory
        }
        long start = System.nanoTime();
        String outcome = "ok";
        try {
          return super.execute(input);
        } catch (RuntimeException e) {
          outcome = "failed (" + e.getClass().getSimpleName() + ")";
          throw e;
        } finally {
          log.info(
              "🔎 {} via={} attempt={} took={}ms outcome={}",
              info.getActivityType(),
              invokedBy(info),
              info.getAttempt(),
              (System.nanoTime() - start) / 1_000_000,
              outcome);
        }
      }
    };
  }

  static boolean isPlatformActivity(String activityType) {
    return activityType.startsWith("notifications.") || activityType.startsWith("payments.");
  }

  /** The same activity code, three ways in: this shows which one. */
  static String invokedBy(ActivityInfo info) {
    return info.getWorkflowId() == null || info.getWorkflowId().isEmpty()
        ? "standalone-activity[" + info.getActivityId() + "]"
        : "workflow[" + info.getWorkflowType() + " " + info.getWorkflowId() + "]";
  }
}
