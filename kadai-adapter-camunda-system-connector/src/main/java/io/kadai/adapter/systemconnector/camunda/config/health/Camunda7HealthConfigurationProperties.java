package io.kadai.adapter.systemconnector.camunda.config.health;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "management.health.kadai-adapter.plugin.camunda7")
public class Camunda7HealthConfigurationProperties
    extends CompositeHealthContributorConfigurationProperties {

  private CompositeHealthContributorConfigurationProperties camunda =
      new CompositeHealthContributorConfigurationProperties();

  private CompositeHealthContributorConfigurationProperties outbox =
      new CompositeHealthContributorConfigurationProperties();

  public CompositeHealthContributorConfigurationProperties getCamunda() {
    return camunda;
  }

  public void setCamunda(CompositeHealthContributorConfigurationProperties camunda) {
    this.camunda = camunda;
  }

  public CompositeHealthContributorConfigurationProperties getOutbox() {
    return outbox;
  }

  public void setOutbox(CompositeHealthContributorConfigurationProperties outbox) {
    this.outbox = outbox;
  }
}
