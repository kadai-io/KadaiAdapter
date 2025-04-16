package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.LastSchedulerRun;
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

  @Override
  public Health health() {
    Instant lastRun = lastSchedulerRun.getLastRunTime();
    return lastRun.isBefore(Instant.now().minus(runInterval))
        ? Health.down().withDetail("Last Run", lastRun).build()
        : Health.up().withDetail("Last Run", lastRun).build();
  }
}
