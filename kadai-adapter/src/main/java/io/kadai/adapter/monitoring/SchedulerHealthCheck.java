package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.LastSchedulerRun;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class SchedulerHealthCheck implements HealthIndicator {

  private final LastSchedulerRun lastSchedulerRun;

  public SchedulerHealthCheck(LastSchedulerRun lastSchedulerRun) {
    this.lastSchedulerRun = lastSchedulerRun;
  }

  @Override
  public Health health() {
    Instant lastRunTime = lastSchedulerRun.getLastRunTime();

    return lastRunTime.isBefore(Instant.now().minus(Duration.ofMinutes(10)))
        ? Health.down().withDetail("Last Run", lastSchedulerRun.getLastRunTime()).build()
        : Health.up().withDetail("Last Run", lastSchedulerRun.getLastRunTime()).build();
  }
}
