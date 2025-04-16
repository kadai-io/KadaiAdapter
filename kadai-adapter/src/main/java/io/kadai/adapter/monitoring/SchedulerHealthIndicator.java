package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.LastSchedulerRun;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class SchedulerHealthIndicator implements HealthIndicator {

  private final LastSchedulerRun lastSchedulerRun;
  private final String name;

  public SchedulerHealthIndicator(LastSchedulerRun lastSchedulerRun, String name) {
    this.lastSchedulerRun = lastSchedulerRun;
    this.name = name;
  }

  @Override
  public Health health() {
    Instant lastRun = lastSchedulerRun.getLastRunTime();
    return lastRun.isBefore(Instant.now().minus(Duration.ofMinutes(10)))
        ? Health.down().withDetail("Last Run", lastRun).build()
        : Health.up().withDetail("Last Run", lastRun).build();
  }
}
