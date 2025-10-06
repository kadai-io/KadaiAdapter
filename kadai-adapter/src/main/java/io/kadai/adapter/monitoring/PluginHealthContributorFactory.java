package io.kadai.adapter.monitoring;

import java.util.Optional;
import org.springframework.boot.actuate.health.HealthContributor;

public interface PluginHealthContributorFactory {
  String getPluginName();

  Optional<HealthContributor> newInstance();
}
