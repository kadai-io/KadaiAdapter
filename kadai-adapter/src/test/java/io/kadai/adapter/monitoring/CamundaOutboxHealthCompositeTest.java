package io.kadai.adapter.monitoring;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.CamundaSystemHealthConfigurationProperties;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CamundaOutboxHealthCompositeTest {

  @ParameterizedTest
  @MethodSource("camundaOutboxHealthConfigurationPropertiesProvider")
  void should_OnlyCreateContributingHealthIndicator_When_Enabled(
      CamundaSystemHealthConfigurationProperties properties, long expectedEnabledCount) {
    final CamundaOutboxHealthComposite camundaOutboxHealthComposite =
        new CamundaOutboxHealthComposite(
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
    final CamundaOutboxHealthComposite camundaOutboxHealthComposite =
        new CamundaOutboxHealthComposite(
            mock(),
            "http://localhost:10020/engine-rest",
            "http://localhost:10020/outbox-rest",
            new CamundaSystemHealthConfigurationProperties());

    assertThat(camundaOutboxHealthComposite.getContributor(contributorName)).isNotNull();
  }

  private static Stream<Arguments> camundaOutboxHealthConfigurationPropertiesProvider() {
    return Stream.of(
        Arguments.of(new CamundaSystemHealthConfigurationProperties(), 2),
        Arguments.of(
            new CamundaSystemHealthConfigurationProperties()
                .withCamunda(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            1),
        Arguments.of(
            new CamundaSystemHealthConfigurationProperties()
                .withOutbox(
                    new CompositeHealthContributorConfigurationProperties().withEnabled(false)),
            1));
  }
}
