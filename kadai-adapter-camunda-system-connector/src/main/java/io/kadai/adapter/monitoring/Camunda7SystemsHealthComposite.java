package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.web.client.RestTemplate;

public class Camunda7SystemsHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public Camunda7SystemsHealthComposite(
      RestTemplate restTemplate,
      List<String> camundaSystemUrls,
      ExternalServicesHealthConfigurationProperties properties) {

    int i = 0;
    if (camundaSystemUrls != null) {
      for (String camundaSystemUrl : camundaSystemUrls) {
        StringTokenizer systemConfigParts = new StringTokenizer(camundaSystemUrl, "|");

        String camundaUrl = systemConfigParts.nextToken().trim();
        String outboxUrl = systemConfigParts.nextToken().trim();

        healthContributors.put(
            "camundaSystem" + ++i,
            new Camunda7OutboxHealthComposite(
                restTemplate, camundaUrl, outboxUrl, properties.getCamundaSystem()));
      }
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
