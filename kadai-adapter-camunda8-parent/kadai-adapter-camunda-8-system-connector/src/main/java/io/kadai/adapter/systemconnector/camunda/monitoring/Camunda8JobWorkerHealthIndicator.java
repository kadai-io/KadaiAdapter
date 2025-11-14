package io.kadai.adapter.systemconnector.camunda.monitoring;

import io.kadai.adapter.monitoring.MonitoredComponent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class Camunda8JobWorkerHealthIndicator implements HealthIndicator {
  private final MonitoredComponent monitoredComponent;

  public Camunda8JobWorkerHealthIndicator(MonitoredComponent monitoredComponent) {
    this.monitoredComponent = monitoredComponent;
  }

  @Override
  public Health health() {
    Instant lastRun = monitoredComponent.getLastRun().getEnd();
    final Map<String, Object> details = new HashMap<>();
    details.put("lastRun", lastRun);
    details.put("expectedRunTime", monitoredComponent.getExpectedRunDuration().toMillis());

    Status status;
    if (lastRun == null || monitoredComponent.getLastRun().isSuccessful() == null) {
      status = Status.UNKNOWN;
    } else {
      status = monitoredComponent.getLastRun().isSuccessful() ? Status.UP : Status.DOWN;
    }

    return Health.status(status).withDetails(details).build();
  }
}
