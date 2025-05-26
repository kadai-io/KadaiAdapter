package io.kadai.adapter.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "outbox-service")
public class OutboxSystemConfigurationProperties {
  private String address = "http://localhost";
  private Integer port = null;
  private String contextPath = null;
  private String endpoint = "outbox-rest/events/count";
  private String query = null;

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }
}
