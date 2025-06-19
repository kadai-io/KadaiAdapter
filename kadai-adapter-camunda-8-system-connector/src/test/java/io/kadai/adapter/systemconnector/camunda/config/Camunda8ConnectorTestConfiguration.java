package io.kadai.adapter.systemconnector.camunda.config;

import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.camunda.tasklistener.ReferencedTaskCreator;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Camunda8ConnectorTestConfiguration {

  @Bean
  AdapterManager adapterManager() {
    return new AdapterManager();
  }

  @Bean
  UserTaskCompletion camundaTaskCompletion(final AdapterManager adapterManager) {
    return new UserTaskCompletion(
        new KadaiTaskTerminator(adapterManager), new ReferencedTaskCreator());
  }
}
