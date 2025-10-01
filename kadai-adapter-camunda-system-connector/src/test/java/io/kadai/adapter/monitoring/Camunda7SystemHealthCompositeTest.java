package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.web.client.RestClient;

class Camunda7SystemHealthCompositeTest {

  @ParameterizedTest
  @ValueSource(strings = {"camundaSystem1", "camundaSystem2"})
  void should_CreateAllContributingHealthIndicatorsByDefaultAndNameThemAccordingToJson(
      String contributorName) {
    final Camunda7SystemsHealthComposite camundaSystemsHealthComposite =
        new Camunda7SystemsHealthComposite(
            mock(),
            List.of(
                "http://localhost:10020/engine-rest| http://localhost:10020/outbox-rest",
                "http://localhost:10021/engine-rest| http://localhost:10021/outbox-rest"),
            new ExternalServicesHealthConfigurationProperties());

    assertThat(camundaSystemsHealthComposite.getContributor(contributorName)).isNotNull();
  }

  @Test
  void should_IterateOverAllHealthContributors() {
    RestClient restTemplate = mock(RestClient.class);
    List<String> urls = List.of(
            "http://localhost:8080/engine|http://localhost:8080/outbox",
            "http://localhost:8081/engine|http://localhost:8081/outbox"
    );
    ExternalServicesHealthConfigurationProperties properties =
            new ExternalServicesHealthConfigurationProperties();

    Camunda7SystemsHealthComposite composite = new Camunda7SystemsHealthComposite(
            restTemplate, urls, properties);

    long count = composite.stream().count();
    assertThat(count).isEqualTo(2);

    List<String> contributorNames = composite.stream()
            .map(NamedContributor::getName)
            .collect(Collectors.toList());

    assertThat(contributorNames).containsExactly("camundaSystem1", "camundaSystem2");
  }
}
