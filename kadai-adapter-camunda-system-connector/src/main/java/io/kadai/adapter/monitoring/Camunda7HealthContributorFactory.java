package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Camunda7HealthContributorFactory implements PluginHealthContributorFactory {

  private final RestClient restClient;
  private final Camunda7HealthConfigurationProperties properties;
  private final List<String> camundaSystemUrls;

  @Autowired
  public Camunda7HealthContributorFactory(
      RestClient restClient,
      Camunda7HealthConfigurationProperties properties,
      @Value("${kadai-system-connector-camundaSystemURLs}") List<String> camundaSystemUrls) {
    this.restClient = restClient;
    this.properties = properties;
    this.camundaSystemUrls = camundaSystemUrls;
  }

  @Override
  public String getPluginName() {
    return "camunda7";
  }

  @Override
  public Optional<HealthContributor> newInstance() {
    return properties.getEnabled()
        ? Optional.of(new Camunda7SystemsHealthComposite(restClient, camundaSystemUrls, properties))
        : Optional.empty();
  }
}
