package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import org.junit.jupiter.api.Test;

public class KernelHealthTest {

  @Test
  void should_DefaultEnabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    final KernelHealth kernelHealth =
        new KernelHealth(kernelHealthConfigurationProperties, null, null, null, null, null);

    assertThat(kernelHealth.getContributor("kadai")).isNotNull();
    assertThat(kernelHealth.getContributor("scheduler")).isNotNull();
  }

  @Test
  void should_IncludeKadaiHealth_When_Enabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final CompositeHealthContributorConfigurationProperties kadaiProperties =
        new CompositeHealthContributorConfigurationProperties();
    kadaiProperties.setEnabled(true);
    kernelHealthConfigurationProperties.setKadai(kadaiProperties);

    final KernelHealth kernelHealth =
        new KernelHealth(kernelHealthConfigurationProperties, null, null, null, null, null);

    assertThat(kernelHealth.getContributor("kadai")).isNotNull();
  }

  @Test
  void should_ExcludeKadaiHealth_When_Disabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final CompositeHealthContributorConfigurationProperties kadaiProperties =
        new CompositeHealthContributorConfigurationProperties();
    kadaiProperties.setEnabled(false);
    kernelHealthConfigurationProperties.setKadai(kadaiProperties);

    final KernelHealth kernelHealth =
        new KernelHealth(kernelHealthConfigurationProperties, null, null, null, null, null);

    assertThat(kernelHealth.getContributor("kadai")).isNull();
  }

  @Test
  void should_IncludeSchedulerHealth_When_Enabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final SchedulerHealthConfigurationProperties schedulerProperties =
        new SchedulerHealthConfigurationProperties();
    schedulerProperties.setEnabled(true);
    kernelHealthConfigurationProperties.setScheduler(schedulerProperties);

    final KernelHealth kernelHealth =
        new KernelHealth(kernelHealthConfigurationProperties, null, null, null, null, null);

    assertThat(kernelHealth.getContributor("scheduler")).isNotNull();
  }

  @Test
  void should_ExcludeSchedulerHealth_When_Disabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final SchedulerHealthConfigurationProperties schedulerProperties =
        new SchedulerHealthConfigurationProperties();
    schedulerProperties.setEnabled(false);
    kernelHealthConfigurationProperties.setScheduler(schedulerProperties);

    final KernelHealth kernelHealth =
        new KernelHealth(kernelHealthConfigurationProperties, null, null, null, null, null);

    assertThat(kernelHealth.getContributor("scheduler")).isNull();
  }
}
