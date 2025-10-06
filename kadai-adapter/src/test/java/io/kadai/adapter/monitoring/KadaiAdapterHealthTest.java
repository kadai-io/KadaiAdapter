package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.health.HealthContributor;

class KadaiAdapterHealthTest {

  @ParameterizedTest
  @MethodSource("coreHealthConfigurationProvider")
  void should_OnlyCreateCoreHealthIndicators_WhenEnabled(
          KernelHealthConfigurationProperties properties,
          long expectedCoreCount) {

    SystemConnectorHealthRegistry mockRegistry = mock(SystemConnectorHealthRegistry.class);
    when(mockRegistry.getEnabledHealthContributors()).thenReturn(Map.of());

    final KadaiAdapterHealth composite = new KadaiAdapterHealth(
            properties,
            mockRegistry,
            mock(), mock(), mock(), mock(), mock());

    final long actual = composite.stream().count();
    assertThat(actual).isEqualTo(expectedCoreCount);
  }

  @Test
  void should_IncludeRegisteredSystemConnectors() {
    SystemConnectorHealthRegistry mockRegistry = mock(SystemConnectorHealthRegistry.class);
    when(mockRegistry.getEnabledHealthContributors()).thenReturn(
            Map.of("mockConnector", mock(HealthContributor.class))
    );

    final KadaiAdapterHealth composite = new KadaiAdapterHealth(
            new KernelHealthConfigurationProperties(),
            mockRegistry,
            mock(), mock(), mock(), mock(), mock());

    assertThat(composite.stream().count()).isEqualTo(3);
    assertThat(composite.getContributor("mockConnector")).isNotNull();
  }

  private static Stream<Arguments> coreHealthConfigurationProvider() {
    return Stream.of(
        Arguments.of(new KernelHealthConfigurationProperties(), 2),
        Arguments.of(
            new KernelHealthConfigurationProperties()
                .withKadai(new CompositeHealthContributorConfigurationProperties()
                        .withEnabled(false)),
                1),
        Arguments.of(
            new KernelHealthConfigurationProperties()
                .withScheduler(
                        (SchedulerHealthConfigurationProperties)
                                new SchedulerHealthConfigurationProperties()
                                        .withEnabled(false)),
            1),
        Arguments.of(
            new KernelHealthConfigurationProperties()
                .withKadai(new CompositeHealthContributorConfigurationProperties()
                        .withEnabled(false))
                        .withScheduler(
                                (SchedulerHealthConfigurationProperties)
                                        new SchedulerHealthConfigurationProperties()
                                                .withEnabled(false)),
                0)
    );
  }
}
