package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.web.client.RestClient;

public class Camunda7OutboxHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public Camunda7OutboxHealthComposite(
      RestClient restClient,
      Camunda7System camunda7System,
      String outboxUrl,
      Camunda7HealthConfigurationProperties properties,
      HttpHeaderProvider httpHeaderProvider) {

    if (properties.getCamunda().getEnabled()) {
      healthContributors.put(
          "camunda", new Camunda7HealthIndicator(restClient, httpHeaderProvider, camunda7System));
    }
    if (properties.getOutbox().getEnabled()) {
      healthContributors.put(
          "outbox", new Camunda7OutboxHealthIndicator(restClient, httpHeaderProvider, outboxUrl));
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
