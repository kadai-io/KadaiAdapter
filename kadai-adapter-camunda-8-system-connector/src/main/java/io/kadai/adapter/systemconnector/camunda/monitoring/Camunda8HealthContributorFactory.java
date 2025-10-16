package io.kadai.adapter.systemconnector.camunda.monitoring;

import io.kadai.adapter.monitoring.PluginHealthContributorFactory;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.stereotype.Component;

@Component
public class Camunda8HealthContributorFactory implements PluginHealthContributorFactory {

  private final Camunda8HealthConfigurationProperties properties;
  private final Camunda8System camunda8System;
  private final UserTaskCompletion userTaskCompletion;
  private final UserTaskCreation userTaskCreation;

  @Autowired
  public Camunda8HealthContributorFactory(
      Camunda8HealthConfigurationProperties properties,
      Camunda8System camunda8System,
      UserTaskCompletion userTaskCompletion,
      UserTaskCreation userTaskCreation) {
    this.properties = properties;
    this.camunda8System = camunda8System;
    this.userTaskCompletion = userTaskCompletion;
    this.userTaskCreation = userTaskCreation;
  }

  @Override
  public String getPluginName() {
    return "camunda8";
  }

  @Override
  public Optional<HealthContributor> newInstance() {
    return properties.getEnabled()
        ? Optional.of(
            new Camunda8SystemHealthComposite(properties, userTaskCompletion, userTaskCreation))
        : Optional.empty();
  }
}
