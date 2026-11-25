package com.example.marketing;

import com.example.platform.api.Platform;
import com.example.platform.api.PlatformClients;
import com.example.platform.api.TemporalConnection;
import com.example.platform.api.model.EmailRequest;
import com.example.platform.api.model.EmailResult;
import io.temporal.client.ActivityClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TEAM MARKETING - consumes platform activities as STANDALONE ACTIVITIES (mode 2).
 *
 * <p>Marketing has no workflows at all. Its campaign tool is plain Java (it could just as well be
 * a REST controller, a cron job or a Lambda). It uses Temporal as a durable job queue: every email
 * is a Standalone Activity with retries, timeouts, de-duplication and visibility - executed by the
 * platform team's worker. Depends on {@code platform-api} only.
 */
public class CampaignSender {
  private static final Logger log = LoggerFactory.getLogger(CampaignSender.class);
  private static final List<String> USERS = List.of("ada", "grace", "linus", "margaret", "ken");

  public static void main(String[] args) {
    String campaign = args.length > 0 ? args[0] : "black-friday-" + System.currentTimeMillis() / 1000;

    // Standalone Activities are started in the namespace whose workers execute them.
    ActivityClient activities = TemporalConnection.activityClient(Platform.NAMESPACE);

    log.info("sending campaign '{}' to {} users as standalone activities...", campaign, USERS.size());
    send(activities, campaign);

    log.info("re-running the SAME campaign (simulates a crashed / retried job runner)...");
    send(activities, campaign); // same idempotency keys -> same results, no duplicate emails

    // Visibility: standalone activities are first-class, queryable executions.
    String query =
        "TaskQueue = '" + Platform.TASK_QUEUE + "' AND ActivityType = 'notifications.SendEmail.v1'";
    log.info(
        "visibility: {} SendEmail standalone executions on '{}'. Most recent:",
        activities.countExecutions(query).getCount(),
        Platform.TASK_QUEUE);
    activities
        .listExecutions(query)
        .limit(8)
        .forEach(a -> log.info("   {}  {}", String.format("%-48s", a.getActivityId()), a.getStatus()));
    System.exit(0);
  }

  private static void send(ActivityClient activities, String campaign) {
    List<CompletableFuture<Void>> sends =
        USERS.stream()
            .map(
                user ->
                    CompletableFuture.runAsync(
                        () -> {
                          Map<String, String> vars = new HashMap<>(Map.of("campaign", campaign));
                          if (user.equals("ada")) vars.put("simulateOutage", "true"); // watch the retry
                          EmailResult r =
                              PlatformClients.sendEmailStandalone(
                                  activities,
                                  new EmailRequest(campaign + ":" + user, user + "@example.com", "promo", vars));
                          log.info("  [standalone] {} -> {}", String.format("%-9s", user), r.messageId());
                        }))
            .toList();
    CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).join();
  }
}
