package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.scheduled.ScheduledComponent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class SchedulerHealthIndicator implements HealthIndicator {

  private final ScheduledComponent scheduledComponent;
  private final long runtimeAcceptanceMultiplier;

  public SchedulerHealthIndicator(
      ScheduledComponent scheduledComponent, long runtimeAcceptanceMultiplier) {
    this.scheduledComponent = scheduledComponent;
    this.runtimeAcceptanceMultiplier = runtimeAcceptanceMultiplier;
  }

  @Override
  public Health health() {
    Instant lastRun = scheduledComponent.getLastSchedulerRun().getRunTime();
    Instant expectedNextRunBefore =
        lastRun
            .plus(scheduledComponent.getRunInterval().multipliedBy(runtimeAcceptanceMultiplier))
            .plus(scheduledComponent.getExpectedRunDuration());
    final Map<String, Object> details = new HashMap<>();
    details.put("lastRun", lastRun);
    details.put("expectedNextRunBefore", expectedNextRunBefore);
    details.put("expectedRunTime", scheduledComponent.getExpectedRunDuration().toMillis());

    final Status status = expectedNextRunBefore.isBefore(Instant.now()) ? Status.DOWN : Status.UP;

    return Health.status(status).withDetails(details).build();
  }
}
