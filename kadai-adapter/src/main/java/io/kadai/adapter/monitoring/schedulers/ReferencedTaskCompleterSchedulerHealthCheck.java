package io.kadai.adapter.monitoring.schedulers;

import io.kadai.adapter.impl.LastSchedulerRun;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("referencedTaskCompleterSchedulerHealthCheck")
public class ReferencedTaskCompleterSchedulerHealthCheck implements HealthIndicator {

  private final LastSchedulerRun lastSchedulerRun;

  public ReferencedTaskCompleterSchedulerHealthCheck(
      @Qualifier("referencedTaskCompleterLastRun") LastSchedulerRun lastSchedulerRun) {
    this.lastSchedulerRun = lastSchedulerRun;
  }

  @Override
  public Health health() {
    Instant lastRun = lastSchedulerRun.getLastRunTime();
    return lastRun.isBefore(Instant.now().minus(Duration.ofMinutes(10)))
        ? Health.down().withDetail("Last Run", lastRun).build()
        : Health.up().withDetail("Last Run", lastRun).build();
  }
}
