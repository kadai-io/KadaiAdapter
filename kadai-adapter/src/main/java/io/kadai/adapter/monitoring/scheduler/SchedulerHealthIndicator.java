package io.kadai.adapter.monitoring.scheduler;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.impl.ScheduledComponent;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class SchedulerHealthIndicator implements HealthIndicator {

  private final LastSchedulerRun lastSchedulerRun;
  private final Duration runInterval;

  public SchedulerHealthIndicator(LastSchedulerRun lastSchedulerRun, Duration runInterval) {
    this.lastSchedulerRun = lastSchedulerRun;
    this.runInterval = runInterval;
  }

  public SchedulerHealthIndicator(ScheduledComponent scheduledComponent) {
    this(scheduledComponent.getLastSchedulerRun(), scheduledComponent.getRunInterval());
  }

  // TODO: Consider time scheduled task takes to run
  //  Might have expected time and average tracked time and last actual time
  //  Should be included in the detail for all statuses

  @Override
  public Health health() {
    Instant lastRun = lastSchedulerRun.getLastRunTime();
    return lastRun.isBefore(Instant.now().minus(runInterval))
        ? Health.down().withDetail("lastRun", lastRun).build()
        : Health.up().withDetail("lastRun", lastRun).build();
  }
}
