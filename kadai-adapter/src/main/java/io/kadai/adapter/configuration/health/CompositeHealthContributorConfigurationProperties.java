package io.kadai.adapter.configuration.health;

public class CompositeHealthContributorConfigurationProperties {

  /** Flag for enabling or disabling this composite health-contributor. */
  private boolean enabled = true;

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public CompositeHealthContributorConfigurationProperties withEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
