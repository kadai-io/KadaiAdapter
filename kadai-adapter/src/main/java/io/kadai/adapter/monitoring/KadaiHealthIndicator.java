package io.kadai.adapter.monitoring;

import io.kadai.KadaiConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class KadaiHealthIndicator implements HealthIndicator {

  @Override
  public Health health() {
    try {
      String version = getCurrentSchemaVersion();
      return Health.up().withDetail("kadaiVersion", version).build();
    } catch (Exception e) {
      return Health.down().withDetail("kadaiServiceError", e.getMessage()).build();
    }
  }

  public String getCurrentSchemaVersion() {
    return KadaiConfiguration.class.getPackage().getImplementationVersion();
  }
}
