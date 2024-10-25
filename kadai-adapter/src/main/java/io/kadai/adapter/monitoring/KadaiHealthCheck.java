package io.kadai.adapter.monitoring;

import io.kadai.KadaiConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class KadaiHealthCheck implements HealthIndicator {

  @Override
  public Health health() {
    try {
      String version = getCurrentSchemaVersion();
      return Health.up().withDetail("Kadai Version", version).build();
    } catch (Exception e) {
      Health health = Health.down().withDetail("Kadai Service Error", e.getMessage()).build();
      return health;
    }
  }

  public String getCurrentSchemaVersion() throws Exception {
    return KadaiConfiguration.class.getPackage().getImplementationVersion();
  }
}
