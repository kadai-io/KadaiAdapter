package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.web.client.RestClient;

public class Camunda7SystemsHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public Camunda7SystemsHealthComposite(
      RestClient restClient,
      List<Camunda7System> camunda7Systems,
      Camunda7HealthConfigurationProperties properties) {

    int i = 0;
    if (camunda7Systems != null) {
      for (Camunda7System camunda7System : camunda7Systems) {
        healthContributors.put(
            "camundaSystem" + ++i,
            new Camunda7OutboxHealthComposite(
                restClient,
                camunda7System.getSystemRestUrl(),
                camunda7System.getSystemTaskEventUrl(),
                properties));
      }
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  public Stream<Entry> stream() {
    return healthContributors.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue()));
  }
}
