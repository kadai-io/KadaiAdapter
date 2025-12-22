package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Camunda7HealthContributorFactory implements PluginHealthContributorFactory {

  private final RestClient restClient;
  private final Camunda7HealthConfigurationProperties properties;
  private final List<Camunda7System> camunda7Systems;

  @Autowired
  public Camunda7HealthContributorFactory(
      RestClient restClient,
      Camunda7HealthConfigurationProperties properties,
      List<Camunda7System> camunda7Systems) {
    this.restClient = restClient;
    this.properties = properties;
    this.camunda7Systems = camunda7Systems;
  }

  @Override
  public String getPluginName() {
    return "camunda7";
  }

  @Override
  public Optional<HealthContributor> newInstance() {
    return properties.getEnabled()
        ? Optional.of(new Camunda7SystemsHealthComposite(restClient, camunda7Systems, properties))
        : Optional.empty();
  }
}
