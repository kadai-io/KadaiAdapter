package io.kadai.adapter.monitoring;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class Camunda7OutboxHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("camundaOutboxHealthConfigurationPropertiesProvider")
  void should_OnlyCreateContributingHealthIndicator_When_Enabled(
      Camunda7HealthConfigurationProperties properties, long expectedEnabledCount) {
    final Camunda7OutboxHealthComposite camundaOutboxHealthComposite =
        new Camunda7OutboxHealthComposite(
            mock(),
            "http://localhost:10020/engine-rest",
            "http://localhost:10020/outbox-rest",
            properties);
    final long actual = camundaOutboxHealthComposite.stream().count();

    assertThat(actual).isEqualTo(expectedEnabledCount);
  }

  @ParameterizedTest
  @ValueSource(strings = {"camunda", "outbox"})
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJson(
      String contributorName) {
    final Camunda7OutboxHealthComposite camundaOutboxHealthComposite =
        new Camunda7OutboxHealthComposite(
            mock(),
            "http://localhost:10020/engine-rest",
            "http://localhost:10020/outbox-rest",
            new Camunda7HealthConfigurationProperties());

    assertThat(camundaOutboxHealthComposite.getContributor(contributorName)).isNotNull();
  }

  private static Stream<Arguments> camundaOutboxHealthConfigurationPropertiesProvider() {
    Camunda7HealthConfigurationProperties allEnabled = new Camunda7HealthConfigurationProperties();

    Camunda7HealthConfigurationProperties camundaDisabled =
        new Camunda7HealthConfigurationProperties();
    camundaDisabled.setCamunda(
        new CompositeHealthContributorConfigurationProperties().withEnabled(false));
    Camunda7HealthConfigurationProperties outboxDisabled =
        new Camunda7HealthConfigurationProperties();
    outboxDisabled.setOutbox(
        new CompositeHealthContributorConfigurationProperties().withEnabled(false));

    return Stream.of(
        Arguments.of(allEnabled, 2),
        Arguments.of(camundaDisabled, 1),
        Arguments.of(outboxDisabled, 1));
  }
}
