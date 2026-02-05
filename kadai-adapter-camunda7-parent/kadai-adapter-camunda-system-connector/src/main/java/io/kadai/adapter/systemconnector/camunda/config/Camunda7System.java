package io.kadai.adapter.systemconnector.camunda.config;

/**
 * Holds the URS (Camunda REST Api and outbox REST) of a specific camunda system as well as an
 * optional identifier for the responsible camunda engine.
 */
public class Camunda7System {

  private String systemRestUrl;
  private String systemTaskEventUrl;
  private String camunda7EngineIdentifier;

  public String getSystemRestUrl() {
    return systemRestUrl;
  }

  public void setSystemRestUrl(String systemRestUrl) {
    this.systemRestUrl = systemRestUrl;
  }

  public String getSystemTaskEventUrl() {
    return systemTaskEventUrl;
  }

  public void setSystemTaskEventUrl(String systemTaskEventUrl) {
    this.systemTaskEventUrl = systemTaskEventUrl;
  }

  public String getCamunda7EngineIdentifier() {
    return camunda7EngineIdentifier;
  }

  public void setCamunda7EngineIdentifier(String camundaEngineIdentifier) {
    this.camunda7EngineIdentifier = camundaEngineIdentifier;
  }

  @Override
  public String toString() {
    return "Camunda7System [systemRestUrl="
        + systemRestUrl
        + ", systemTaskEventUrl="
        + systemTaskEventUrl
        + ", camunda7EngineIdentifier="
        + camunda7EngineIdentifier
        + "]";
  }
}
