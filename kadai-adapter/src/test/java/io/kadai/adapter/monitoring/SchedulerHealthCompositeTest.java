package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SchedulerHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("schedulerHealthConfigurationPropertiesProvider")
  void should_OnlyCreateContributingHealthIndicator_WhenEnabled(
      SchedulerHealthConfigurationProperties properties, long expectedEnabledCount) {
    final SchedulerHealthComposite schedulerHealthComposite =
        new SchedulerHealthComposite(properties, mock(), mock(), mock(), mock(), mock());

    final long actual = schedulerHealthComposite.stream().count();

    assertThat(actual).isEqualTo(expectedEnabledCount);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "referencedTaskCompleter",
        "referencedTaskClaimer",
        "referencedTaskClaimCanceler",
        "kadaiTaskStarter",
        "kadaiTaskTerminator"
      })
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJson(
      String contributorName) {
    final SchedulerHealthComposite schedulerHealthComposite =
        new SchedulerHealthComposite(
            new SchedulerHealthConfigurationProperties(), mock(), mock(), mock(), mock(), mock());

    assertThat(schedulerHealthComposite.getContributor(contributorName)).isNotNull();
  }

  private static Stream<Arguments> schedulerHealthConfigurationPropertiesProvider() {
    return Stream.of(
        Arguments.of(new SchedulerHealthConfigurationProperties(), 5),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withKadaiTaskStarter(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            4),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withKadaiTaskTerminator(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            4),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withReferencedTaskCompleter(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            4),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withReferencedTaskClaimer(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            4),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withReferencedTaskClaimCanceler(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            4),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withReferencedTaskClaimer(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withKadaiTaskTerminator(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            3),
        Arguments.of(
            new SchedulerHealthConfigurationProperties()
                .withReferencedTaskCompleter(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withReferencedTaskClaimer(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withReferencedTaskClaimCanceler(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withKadaiTaskStarter(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false))
                .withKadaiTaskTerminator(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            0));
  }
}
