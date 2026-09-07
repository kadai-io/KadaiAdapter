package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Camunda7HealthContributorFactory implements PluginHealthContributorFactory {

  private final ExternalServiceHttpProbe httpProbe;
  private final Camunda7HealthConfigurationProperties properties;
  private final List<Camunda7System> camunda7Systems;
  private final HttpHeaderProvider httpHeaderProvider;

  @Autowired
  public Camunda7HealthContributorFactory(
      RestClient restClient,
      JsonMapper jsonMapper,
      Camunda7HealthConfigurationProperties properties,
      List<Camunda7System> camunda7Systems,
      HttpHeaderProvider httpHeaderProvider) {
    this.httpProbe = new ExternalServiceHttpProbe(restClient, jsonMapper);
    this.properties = properties;
    this.camunda7Systems = camunda7Systems;
    this.httpHeaderProvider = httpHeaderProvider;
  }

  @Override
  public String getPluginName() {
    return "camunda7";
  }

  @Override
  public Optional<HealthContributor> newInstance() {
    return properties.getEnabled()
        ? Optional.of(
            new Camunda7SystemsHealthComposite(
                httpProbe, camunda7Systems, properties, httpHeaderProvider))
        : Optional.empty();
  }
}
