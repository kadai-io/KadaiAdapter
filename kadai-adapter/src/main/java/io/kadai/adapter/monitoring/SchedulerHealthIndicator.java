package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.ScheduledComponent;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class SchedulerHealthIndicator implements HealthIndicator {

  private final ScheduledComponent scheduledComponent;

  public SchedulerHealthIndicator(ScheduledComponent scheduledComponent) {
    this.scheduledComponent = scheduledComponent;
  }

  // TODO: Enhance detail

  @Override
  public Health health() {
    Instant lastRun = scheduledComponent.getLastSchedulerRun().getRunTime();
    return lastRun.isBefore(
            Instant.now()
                .minus(scheduledComponent.getRunInterval())
                .minus(scheduledComponent.getExpectedRunDuration()))
        ? Health.down().withDetail("lastRun", lastRun).build()
        : Health.up().withDetail("lastRun", lastRun).build();
  }
}
