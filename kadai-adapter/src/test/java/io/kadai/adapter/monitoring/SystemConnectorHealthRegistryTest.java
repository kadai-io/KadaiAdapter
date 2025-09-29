package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthContributor;

class SystemConnectorHealthRegistryTest {

  @Test
  void should_RegisterAndReturnEnabledContributors() {

    SystemConnectorHealthContributor enabledContributor = mock(
            SystemConnectorHealthContributor.class);
    when(enabledContributor.isEnabled()).thenReturn(true);
    when(enabledContributor.getConnectorName()).thenReturn("testConnector");
    when(enabledContributor.createHealthContributor()).thenReturn(mock(HealthContributor.class));

    SystemConnectorHealthContributor disabledContributor = mock(
            SystemConnectorHealthContributor.class);
    when(disabledContributor.isEnabled()).thenReturn(false);

    SystemConnectorHealthRegistry registry = new SystemConnectorHealthRegistry();
    registry.registerContributor(enabledContributor);
    registry.registerContributor(disabledContributor);

    Map<String, HealthContributor> result = registry.getEnabledHealthContributors();

    assertThat(result).hasSize(1);
    assertThat(result).containsKey("testConnector");
  }

  @Test
  void should_ReturnEmptyMap_When_NoContributorsRegistered() {
    SystemConnectorHealthRegistry registry = new SystemConnectorHealthRegistry();

    assertThat(registry.getEnabledHealthContributors()).isEmpty();
  }
}
