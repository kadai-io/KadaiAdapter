package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "management.health.external-services")
public class ExternalServicesHealthConfigurationProperties {

  private CompositeHealthContributorConfigurationProperties camunda =
      new CompositeHealthContributorConfigurationProperties();
  private CompositeHealthContributorConfigurationProperties outbox =
      new CompositeHealthContributorConfigurationProperties();
  private CompositeHealthContributorConfigurationProperties kadai =
      new CompositeHealthContributorConfigurationProperties();
  private CompositeHealthContributorConfigurationProperties scheduler =
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

  public CompositeHealthContributorConfigurationProperties getKadai() {
    return kadai;
  }

  public void setKadai(CompositeHealthContributorConfigurationProperties kadai) {
    this.kadai = kadai;
  }

  public CompositeHealthContributorConfigurationProperties getScheduler() {
    return scheduler;
  }

  public void setScheduler(CompositeHealthContributorConfigurationProperties scheduler) {
    this.scheduler = scheduler;
  }
}
