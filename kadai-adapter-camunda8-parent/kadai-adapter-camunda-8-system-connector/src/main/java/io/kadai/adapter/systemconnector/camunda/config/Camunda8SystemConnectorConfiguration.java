package io.kadai.adapter.systemconnector.camunda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("kadai-adapter.plugin.camunda8")
public class Camunda8SystemConnectorConfiguration {

  /** Configuration for claiming in Camunda 8. */
  private ClaimingConfiguration claiming = new ClaimingConfiguration();

  /** Configuration for completing in Camunda 8. */
  private CompletingConfiguration completing = new CompletingConfiguration();

  public ClaimingConfiguration getClaiming() {
    return claiming;
  }

  public void setClaiming(ClaimingConfiguration claiming) {
    this.claiming = claiming;
  }

  public CompletingConfiguration getCompleting() {
    return completing;
  }

  public void setCompleting(CompletingConfiguration completing) {
    this.completing = completing;
  }

  public static class ClaimingConfiguration {
    /**
     * Flag for enabling or disabling Claiming as part of the KadaiAdapter synchronization with
     * Camunda 8.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class CompletingConfiguration {
    /**
     * Flag for enabling or disabling Completing as part of the KadaiAdapter synchronization with
     * Camunda 8.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
