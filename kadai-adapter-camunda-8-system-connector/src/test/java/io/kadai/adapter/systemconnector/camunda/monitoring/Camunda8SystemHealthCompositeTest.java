package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties.JobWorkerHealthConfigurationProperties;
import org.junit.jupiter.api.Test;

class Camunda8SystemHealthCompositeTest {

  @Test
  void should_DefaultJobWorkerEnabled() {
    final Camunda8HealthConfigurationProperties camunda8HealthConfigurationProperties =
        new Camunda8HealthConfigurationProperties();
    final Camunda8SystemHealthComposite camunda8SystemHealthComposite =
        new Camunda8SystemHealthComposite(camunda8HealthConfigurationProperties, null, null, null);

    assertThat(camunda8SystemHealthComposite.getContributor("jobWorker")).isNotNull();
  }

  @Test
  void should_EnableJobWorker_When_Enabled() {
    final Camunda8HealthConfigurationProperties camunda8HealthConfigurationProperties =
        new Camunda8HealthConfigurationProperties();
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    jobWorkerHealthConfigurationProperties.setEnabled(true);
    camunda8HealthConfigurationProperties.setJobWorker(jobWorkerHealthConfigurationProperties);
    final Camunda8SystemHealthComposite camunda8SystemHealthComposite =
        new Camunda8SystemHealthComposite(camunda8HealthConfigurationProperties, null, null, null);

    assertThat(camunda8SystemHealthComposite.getContributor("jobWorker")).isNotNull();
  }

  @Test
  void should_DisableJobWorker_When_Enabled() {
    final Camunda8HealthConfigurationProperties camunda8HealthConfigurationProperties =
        new Camunda8HealthConfigurationProperties();
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    jobWorkerHealthConfigurationProperties.setEnabled(false);
    camunda8HealthConfigurationProperties.setJobWorker(jobWorkerHealthConfigurationProperties);
    final Camunda8SystemHealthComposite camunda8SystemHealthComposite =
        new Camunda8SystemHealthComposite(camunda8HealthConfigurationProperties, null, null, null);

    assertThat(camunda8SystemHealthComposite.getContributor("jobWorker")).isNull();
  }
}
