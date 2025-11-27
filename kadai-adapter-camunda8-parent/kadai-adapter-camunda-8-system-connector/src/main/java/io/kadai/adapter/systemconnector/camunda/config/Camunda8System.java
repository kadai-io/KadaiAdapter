package io.kadai.adapter.systemconnector.camunda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Camunda8System {

  @Value("${camunda.client.rest-address}")
  private String restAddress;

  public String getRestAddress() {
    return restAddress;
  }

  public void setRestAddress(String restAddress) {
    this.restAddress = restAddress;
  }

  // Hard-coded now until multiple C8-Systems are supported, configured OR derived then
  public int getIdentifier() {
    return 0;
  }
}
