package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.CamundaSystemHealthConfigurationProperties;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.web.client.RestTemplate;

public class CamundaOutboxHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public CamundaOutboxHealthComposite(
      RestTemplate restTemplate,
      String camundaUrl,
      String outboxUrl,
      CamundaSystemHealthConfigurationProperties properties) {
    if (properties.getCamunda().getEnabled()) {
      healthContributors.put(
          "camunda", new CamundaHealthIndicator(restTemplate, camundaUrl));
    }
    if (properties.getOutbox().getEnabled()) {
      healthContributors.put("outbox", new OutboxHealthIndicator(restTemplate, outboxUrl));
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
