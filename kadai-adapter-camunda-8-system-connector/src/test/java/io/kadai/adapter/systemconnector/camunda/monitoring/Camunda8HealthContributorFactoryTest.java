package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties;
import org.junit.jupiter.api.Test;

class Camunda8HealthContributorFactoryTest {

  @Test
  void should_DefaultEnabled() {
    final Camunda8HealthConfigurationProperties camunda8HealthConfigurationProperties =
        new Camunda8HealthConfigurationProperties();
    final Camunda8HealthContributorFactory camunda8HealthContributorFactory =
        new Camunda8HealthContributorFactory(
            camunda8HealthConfigurationProperties, null, null);

    assertThat(camunda8HealthContributorFactory.newInstance()).isPresent();
  }
}
