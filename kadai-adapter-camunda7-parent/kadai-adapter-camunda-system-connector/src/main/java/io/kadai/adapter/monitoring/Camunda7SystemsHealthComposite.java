package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.web.client.RestClient;

public class Camunda7SystemsHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new LinkedHashMap<>();

  public Camunda7SystemsHealthComposite(
      RestClient restClient,
      List<Camunda7System> camunda7Systems,
      Camunda7HealthConfigurationProperties properties,
      HttpHeaderProvider httpHeaderProvider) {

    int i = 0;
    if (camunda7Systems != null) {
      for (Camunda7System camunda7System : camunda7Systems) {
        String contributorName =
            uniqueContributorName(determineContributorName(camunda7System, ++i));
        healthContributors.put(
            contributorName,
            new Camunda7OutboxHealthComposite(
                restClient,
                camunda7System,
                camunda7System.getSystemTaskEventUrl(),
                properties,
                httpHeaderProvider));
      }
    }
  }

  private String uniqueContributorName(String contributorName) {
    if (!healthContributors.containsKey(contributorName)) {
      return contributorName;
    }

    int duplicateIndex = 2;
    String uniqueContributorName;
    do {
      uniqueContributorName = contributorName + "-" + duplicateIndex++;
    } while (healthContributors.containsKey(uniqueContributorName));
    return uniqueContributorName;
  }

  private static String determineContributorName(Camunda7System camunda7System, int systemIndex) {
    String engineIdentifier = camunda7System.getCamunda7EngineIdentifier();
    if (engineIdentifier != null && !engineIdentifier.isBlank()) {
      return engineIdentifier;
    }
    return "camundaSystem" + systemIndex;
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
