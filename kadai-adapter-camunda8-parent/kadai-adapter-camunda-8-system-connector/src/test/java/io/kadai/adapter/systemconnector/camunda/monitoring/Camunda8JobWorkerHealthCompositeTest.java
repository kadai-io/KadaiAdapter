package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties.JobWorkerHealthConfigurationProperties;
import org.junit.jupiter.api.Test;

class Camunda8JobWorkerHealthCompositeTest {

  @Test
  void should_DefaultAllJobWorkersEnabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("complete")).isNotNull();
    assertThat(camunda8JobWorkerHealthComposite.getContributor("create")).isNotNull();
  }

  @Test
  void should_EnableCompletedListener_WhenEnabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(true);
    jobWorkerHealthConfigurationProperties.setComplete(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("complete")).isNotNull();
  }

  @Test
  void should_DisableCompletedListener_WhenDisabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(false);
    jobWorkerHealthConfigurationProperties.setComplete(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("complete")).isNull();
  }

  @Test
  void should_EnableCreateListener_WhenEnabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(true);
    jobWorkerHealthConfigurationProperties.setCreate(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("create")).isNotNull();
  }

  @Test
  void should_DisableCreatedListener_WhenDisabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(false);
    jobWorkerHealthConfigurationProperties.setCreate(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("create")).isNull();
  }

  @Test
  void should_EnableCancelListener_WhenEnabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(true);
    jobWorkerHealthConfigurationProperties.setCancel(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("cancel")).isNotNull();
  }

  @Test
  void should_DisableCancelListener_WhenDisabled() {
    final JobWorkerHealthConfigurationProperties jobWorkerHealthConfigurationProperties =
        new JobWorkerHealthConfigurationProperties();
    final CompositeHealthContributorConfigurationProperties listenerProperties =
        new CompositeHealthContributorConfigurationProperties();
    listenerProperties.setEnabled(false);
    jobWorkerHealthConfigurationProperties.setCancel(listenerProperties);
    final Camunda8JobWorkerHealthComposite camunda8JobWorkerHealthComposite =
        new Camunda8JobWorkerHealthComposite(
            jobWorkerHealthConfigurationProperties, null, null, null);

    assertThat(camunda8JobWorkerHealthComposite.getContributor("cancel")).isNull();
  }
}
