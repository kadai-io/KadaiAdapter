package io.kadai.adapter.systemconnector.camunda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kadai.adapter.plugin.camunda8")
public class Camunda8System {

  private String systemUrl;

  public String getSystemUrl() {
    return systemUrl;
  }

  public void setSystemUrl(String systemUrl) {
    this.systemUrl = systemUrl;
  }
}
