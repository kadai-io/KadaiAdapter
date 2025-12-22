package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
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
    final Camunda7System camunda7System1 = new Camunda7System();
    camunda7System1.setSystemRestUrl("http://localhost:8080/engine");
    camunda7System1.setSystemTaskEventUrl("http://localhost:8080/outbox");

    final Camunda7System camunda7System2 = new Camunda7System();
    camunda7System2.setSystemRestUrl("http://localhost:8081/engine");
    camunda7System2.setSystemTaskEventUrl("http://localhost:8081/outbox");

    final Camunda7SystemsHealthComposite camundaSystemsHealthComposite =
        new Camunda7SystemsHealthComposite(
            mock(),
            List.of(camunda7System1, camunda7System2),
            new Camunda7HealthConfigurationProperties());

    assertThat(camundaSystemsHealthComposite.getContributor(contributorName)).isNotNull();
  }

  @Test
  void should_IterateOverAllHealthContributors() {
    RestClient restTemplate = mock(RestClient.class);
    final Camunda7System camunda7System1 = new Camunda7System();
    camunda7System1.setSystemRestUrl("http://localhost:8080/engine");
    camunda7System1.setSystemTaskEventUrl("http://localhost:8080/outbox");

    final Camunda7System camunda7System2 = new Camunda7System();
    camunda7System2.setSystemRestUrl("http://localhost:8081/engine");
    camunda7System2.setSystemTaskEventUrl("http://localhost:8081/outbox");
    List<Camunda7System> urls = List.of(camunda7System1, camunda7System2);

    Camunda7HealthConfigurationProperties properties = new Camunda7HealthConfigurationProperties();

    Camunda7SystemsHealthComposite composite =
        new Camunda7SystemsHealthComposite(restTemplate, urls, properties);

    long count = composite.stream().count();
    assertThat(count).isEqualTo(2);

    List<String> contributorNames =
        composite.stream().map(NamedContributor::getName).collect(Collectors.toList());

    assertThat(contributorNames).containsExactly("camundaSystem1", "camundaSystem2");
  }
}
