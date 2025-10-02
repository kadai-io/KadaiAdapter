package io.kadai.adapter.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "kadai.adapter.camunda.system.enabled", havingValue = "true")
public class Camunda7HealthContributor implements SystemConnectorHealthContributor {

  private final RestClient restClient;
  private final Camunda7HealthConfigurationProperties properties;
  private final List<String> camundaSystemUrls;

  @Autowired
  public Camunda7HealthContributor(
          SystemConnectorHealthRegistry registry,
          RestClient restClient,
          Camunda7HealthConfigurationProperties properties,
          @Value("${kadai-system-connector-camundaSystemURLs}") List<String> camundaSystemUrls) {
    this.restClient = restClient;
    this.properties = properties;
    this.camundaSystemUrls = camundaSystemUrls;

    registry.registerContributor(this);
  }

  @Override
  public String getConnectorName() {
    return "camundaSystems";
  }

  @Override
  public HealthContributor createHealthContributor() {
    return new Camunda7SystemsHealthComposite(restClient, camundaSystemUrls, properties);
  }

  @Override
  public boolean isEnabled() {
    return properties.getEnabled();
  }
}
