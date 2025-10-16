package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.scheduled.MonitoredScheduledComponent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class SchedulerHealthIndicator implements HealthIndicator {

  private final MonitoredScheduledComponent monitoredScheduledComponent;
  private final long runtimeAcceptanceMultiplier;

  public SchedulerHealthIndicator(
      MonitoredScheduledComponent monitoredScheduledComponent, long runtimeAcceptanceMultiplier) {
    this.monitoredScheduledComponent = monitoredScheduledComponent;
    this.runtimeAcceptanceMultiplier = runtimeAcceptanceMultiplier;
  }

  @Override
  public Health health() {
    Instant lastRun = monitoredScheduledComponent.getLastRun().getEnd();
    Instant expectedNextRunBefore =
        lastRun
            .plus(
                monitoredScheduledComponent
                    .getRunInterval()
                    .multipliedBy(runtimeAcceptanceMultiplier))
            .plus(monitoredScheduledComponent.getExpectedRunDuration());
    final Map<String, Object> details = new HashMap<>();
    details.put("lastRun", lastRun);
    details.put("expectedNextRunBefore", expectedNextRunBefore);
    details.put("expectedRunTime", monitoredScheduledComponent.getExpectedRunDuration().toMillis());

    final Status status = expectedNextRunBefore.isBefore(Instant.now()) ? Status.DOWN : Status.UP;

    return Health.status(status).withDetails(details).build();
  }
}
