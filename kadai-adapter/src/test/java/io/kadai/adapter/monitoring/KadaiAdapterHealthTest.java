package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.PluginHealthConfigurationProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class KadaiAdapterHealthTest {

  @Test
  void should_DefaultEnabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    final PluginHealthConfigurationProperties pluginHealthConfigurationProperties =
        new PluginHealthConfigurationProperties();

    final KadaiAdapterHealth kadaiAdapterHealth =
        new KadaiAdapterHealth(
            kernelHealthConfigurationProperties,
            null,
            null,
            null,
            null,
            null,
            pluginHealthConfigurationProperties,
            Collections.emptyList());

    assertThat(kadaiAdapterHealth.getContributor("kernel")).isNotNull();
    assertThat(kadaiAdapterHealth.getContributor("plugin")).isNotNull();
  }

  @Test
  void should_IncludeKernelHealth_When_Enabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final PluginHealthConfigurationProperties pluginHealthConfigurationProperties =
        new PluginHealthConfigurationProperties();
    pluginHealthConfigurationProperties.setEnabled(true);

    final KadaiAdapterHealth kadaiAdapterHealth =
        new KadaiAdapterHealth(
            kernelHealthConfigurationProperties,
            null,
            null,
            null,
            null,
            null,
            pluginHealthConfigurationProperties,
            Collections.emptyList());

    assertThat(kadaiAdapterHealth.getContributor("kernel")).isNotNull();
  }

  @Test
  void should_ExcludeKernelHealth_When_Disabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(false);
    final PluginHealthConfigurationProperties pluginHealthConfigurationProperties =
        new PluginHealthConfigurationProperties();
    pluginHealthConfigurationProperties.setEnabled(true);

    final KadaiAdapterHealth kadaiAdapterHealth =
        new KadaiAdapterHealth(
            kernelHealthConfigurationProperties,
            null,
            null,
            null,
            null,
            null,
            pluginHealthConfigurationProperties,
            Collections.emptyList());

    assertThat(kadaiAdapterHealth.getContributor("kernel")).isNull();
  }

  @Test
  void should_IncludePluginHealth_When_Enabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final PluginHealthConfigurationProperties pluginHealthConfigurationProperties =
        new PluginHealthConfigurationProperties();
    pluginHealthConfigurationProperties.setEnabled(true);

    final KadaiAdapterHealth kadaiAdapterHealth =
        new KadaiAdapterHealth(
            kernelHealthConfigurationProperties,
            null,
            null,
            null,
            null,
            null,
            pluginHealthConfigurationProperties,
            Collections.emptyList());

    assertThat(kadaiAdapterHealth.getContributor("plugin")).isNotNull();
  }

  @Test
  void should_ExcludePluginHealth_When_Disabled() {
    final KernelHealthConfigurationProperties kernelHealthConfigurationProperties =
        new KernelHealthConfigurationProperties();
    kernelHealthConfigurationProperties.setEnabled(true);
    final PluginHealthConfigurationProperties pluginHealthConfigurationProperties =
        new PluginHealthConfigurationProperties();
    pluginHealthConfigurationProperties.setEnabled(false);

    final KadaiAdapterHealth kadaiAdapterHealth =
        new KadaiAdapterHealth(
            kernelHealthConfigurationProperties,
            null,
            null,
            null,
            null,
            null,
            pluginHealthConfigurationProperties,
            Collections.emptyList());

    assertThat(kadaiAdapterHealth.getContributor("plugin")).isNull();
  }
}
