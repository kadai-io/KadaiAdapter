package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CamundaSystemHealthCompositeTest {

  @ParameterizedTest
  @ValueSource(strings = {"camundaSystem1", "camundaSystem2"})
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJson(
      String contributorName) {
    final CamundaSystemsHealthComposite camundaSystemsHealthComposite =
        new CamundaSystemsHealthComposite(
            mock(),
            List.of(
                "http://localhost:10020/engine-rest| http://localhost:10020/outbox-rest",
                "http://localhost:10021/engine-rest| http://localhost:10021/outbox-rest"),
            new ExternalServicesHealthConfigurationProperties());

    assertThat(camundaSystemsHealthComposite.getContributor(contributorName)).isNotNull();
  }
}
