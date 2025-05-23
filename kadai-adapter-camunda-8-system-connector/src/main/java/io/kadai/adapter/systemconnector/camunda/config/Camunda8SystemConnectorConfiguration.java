package io.kadai.adapter.systemconnector.camunda.config;

import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.camunda.task.listener.UserTaskListener;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Configures the camunda system connector. */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
public class Camunda8SystemConnectorConfiguration {

  @Bean
  RestTemplate restTemplate(
      RestTemplateBuilder builder, HttpComponentsClientProperties httpComponentsClientProperties) {
    return builder
        .connectTimeout(Duration.ofMillis(httpComponentsClientProperties.getConnectionTimeout()))
        .readTimeout(Duration.ofMillis(httpComponentsClientProperties.getReadTimeout()))
        .requestFactory(HttpComponentsClientHttpRequestFactory.class)
        .build();
  }

  @Bean
  Integer getFromKadaiToAdapterBatchSize(
      @Value("${kadai.adapter.sync.kadai.batchSize:#{64}}") final Integer batchSize) {
    return batchSize;
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  UserTaskListener userTaskListener(
      final AdapterManager adapterManager, final LastSchedulerRun lastSchedulerRun) {
    return new UserTaskListener(new KadaiTaskTerminator(adapterManager, lastSchedulerRun));
  }
}
