package io.kadai.adapter.monitoring;

import org.springframework.boot.actuate.health.HealthContributor;

public interface SystemConnectorHealthContributor {
  String getConnectorName();

  HealthContributor createHealthContributor();

  boolean isEnabled();
}
