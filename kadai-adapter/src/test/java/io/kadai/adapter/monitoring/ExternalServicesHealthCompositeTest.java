package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.health.HealthContributor;

class ExternalServicesHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("coreHealthConfigurationProvider")
  void should_OnlyCreateCoreHealthIndicators_WhenEnabled(
          ExternalServicesHealthConfigurationProperties properties,
          long expectedCoreCount) {

    SystemConnectorHealthRegistry mockRegistry = mock(SystemConnectorHealthRegistry.class);
    when(mockRegistry.getEnabledHealthContributors()).thenReturn(Map.of());

    final ExternalServicesHealthComposite composite = new ExternalServicesHealthComposite(
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

    final ExternalServicesHealthComposite composite = new ExternalServicesHealthComposite(
            new ExternalServicesHealthConfigurationProperties(),
            mockRegistry,
            mock(), mock(), mock(), mock(), mock());

    assertThat(composite.stream().count()).isEqualTo(3);
    assertThat(composite.getContributor("mockConnector")).isNotNull();
  }

  private static Stream<Arguments> coreHealthConfigurationProvider() {
    return Stream.of(
        Arguments.of(new ExternalServicesHealthConfigurationProperties(), 2),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withKadai(new CompositeHealthContributorConfigurationProperties()
                        .withEnabled(false)),
                1),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withScheduler(
                        (SchedulerHealthConfigurationProperties)
                                new SchedulerHealthConfigurationProperties()
                                        .withEnabled(false)),
            1),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
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
