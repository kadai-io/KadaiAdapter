package io.kadai.adapter.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.stereotype.Component;

@Component
public class SystemConnectorHealthRegistry {
  private final List<SystemConnectorHealthContributor> contributors = new ArrayList<>();

  public void registerContributor(SystemConnectorHealthContributor contributor) {
    contributors.add(contributor);
  }

  public Map<String, HealthContributor> getEnabledHealthContributors() {
    return contributors.stream()
            .filter(SystemConnectorHealthContributor::isEnabled)
            .collect(Collectors.toMap(
                    SystemConnectorHealthContributor::getConnectorName,
                    SystemConnectorHealthContributor::createHealthContributor
            ));
  }
}
