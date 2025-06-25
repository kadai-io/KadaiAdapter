package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.CamundaSystemHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExternalServicesHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("externalServicesHealthConfigurationPropertiesProvider")
  void should_OnlyCreateContributingHealthIndicator_WhenEnabled(
      ExternalServicesHealthConfigurationProperties properties, long expectedEnabledCount) {
    final ExternalServicesHealthComposite externalServicesHealthComposite =
        new ExternalServicesHealthComposite(
            properties,
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            List.of(
                "http://localhost:10020/engine-rest| http://localhost:10020/outbox-rest",
                "http://localhost:10021/engine-rest| http://localhost:10021/outbox-rest"));

    final long actual = externalServicesHealthComposite.stream().count();

    assertThat(actual).isEqualTo(expectedEnabledCount);
  }

  @ParameterizedTest
  @ValueSource(strings = {"camundaSystems", "kadai", "scheduler"})
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJson(
      String contributorName) {
    final ExternalServicesHealthComposite externalServicesHealthComposite =
        new ExternalServicesHealthComposite(
            new ExternalServicesHealthConfigurationProperties(),
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            mock(),
            List.of(
                "http://localhost:10020/engine-rest| http://localhost:10020/outbox-rest",
                "http://localhost:10021/engine-rest| http://localhost:10021/outbox-rest"));

    assertThat(externalServicesHealthComposite.getContributor(contributorName)).isNotNull();
  }

  private static Stream<Arguments> externalServicesHealthConfigurationPropertiesProvider() {
    return Stream.of(
        Arguments.of(new ExternalServicesHealthConfigurationProperties(), 3),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamundaSystem(
                    (CamundaSystemHealthConfigurationProperties)
                        new CamundaSystemHealthConfigurationProperties().withEnabled(false)),
            2),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            2),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withScheduler(
                    (SchedulerHealthConfigurationProperties)
                        new SchedulerHealthConfigurationProperties().withEnabled(false)),
            2),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamundaSystem(
                    (CamundaSystemHealthConfigurationProperties)
                        new CamundaSystemHealthConfigurationProperties().withEnabled(false))
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            1),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamundaSystem(
                    (CamundaSystemHealthConfigurationProperties)
                        new CamundaSystemHealthConfigurationProperties().withEnabled(false))
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withScheduler(
                    (SchedulerHealthConfigurationProperties)
                        new SchedulerHealthConfigurationProperties().withEnabled(false)),
            0));
  }
}
