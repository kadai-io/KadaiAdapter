package io.kadai.adapter.systemconnector.camunda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kadai.adapter.plugin.camunda8")
public class Camunda8System {

  private String systemUrl;
  private String clusterApiUrl;

  public String getSystemUrl() {
    return systemUrl;
  }

  public void setSystemUrl(String systemUrl) {
    this.systemUrl = systemUrl;
  }

  public String getClusterApiUrl() {
    return clusterApiUrl;
  }

  public void setClusterApiUrl(String clusterApiUrl) {
    this.clusterApiUrl = clusterApiUrl;
  }

  // Hard-coded now until multiple C8-Systems are supported, configured OR derived then
  public int getIdentifier() {
    return 0;
  }
}
