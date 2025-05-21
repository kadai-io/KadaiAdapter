package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ExternalServicesHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("externalServicesHealthConfigurationPropertiesProvider")
  void should_OnlyCreateContributingHealthIndicator_WhenEnabled(
      ExternalServicesHealthConfigurationProperties properties, long expectedEnabledCount) {
    final ExternalServicesHealthComposite externalServicesHealthComposite =
        new ExternalServicesHealthComposite(
            properties, mock(), "foo", 42, "bar", mock(), mock(), mock(), mock(), mock());

    final long actual = externalServicesHealthComposite.stream().count();

    assertThat(actual).isEqualTo(expectedEnabledCount);
  }

  @ParameterizedTest
  @ValueSource(strings = {"camunda", "kadai", "outbox", "scheduler"})
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJSON(
      String contributorName) {
    final ExternalServicesHealthComposite externalServicesHealthComposite =
        new ExternalServicesHealthComposite(
            new ExternalServicesHealthConfigurationProperties(),
            mock(),
            "foo",
            42,
            "bar",
            mock(),
            mock(),
            mock(),
            mock(),
            mock());

    assertThat(externalServicesHealthComposite.getContributor(contributorName)).isNotNull();
  }

  private static Stream<Arguments> externalServicesHealthConfigurationPropertiesProvider() {
    return Stream.of(
        Arguments.of(new ExternalServicesHealthConfigurationProperties(), 4),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamunda(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            3),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            3),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withOutbox(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            3),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withScheduler(
                    (SchedulerHealthConfigurationProperties)
                        new SchedulerHealthConfigurationProperties().withEnabled(false)),
            3),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamunda(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            2),
        Arguments.of(
            new ExternalServicesHealthConfigurationProperties()
                .withCamunda(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withKadai(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withOutbox(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withScheduler(
                    (SchedulerHealthConfigurationProperties)
                        new SchedulerHealthConfigurationProperties().withEnabled(false)),
            0));
  }
}
