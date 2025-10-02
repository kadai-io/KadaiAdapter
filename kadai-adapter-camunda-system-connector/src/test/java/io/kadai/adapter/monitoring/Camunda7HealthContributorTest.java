package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda7HealthConfigurationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class Camunda7HealthContributorTest {

  @Test
  void should_CreateCamundaSystemsHealthComposite_When_Enabled() {
    Camunda7HealthConfigurationProperties properties =
            new Camunda7HealthConfigurationProperties();

    List<String> camundaUrls = List.of(
            "http://localhost:10020/engine-rest|http://localhost:10020/outbox-rest"
    );

    Camunda7HealthContributor contributor = new Camunda7HealthContributor(
            mock(SystemConnectorHealthRegistry.class),
            mock(RestClient.class),
            properties,
            camundaUrls);

    assertThat(contributor.getConnectorName()).isEqualTo("camundaSystems");
    assertThat(contributor.createHealthContributor())
            .isInstanceOf(Camunda7SystemsHealthComposite.class);
    assertThat(contributor.isEnabled()).isTrue();
  }

  @Test
  void should_BeDisabled_When_CamundaSystemDisabled() {
    Camunda7HealthConfigurationProperties properties =
            new Camunda7HealthConfigurationProperties();
    properties.setEnabled(false);

    List<String> camundaUrls = List.of(
            "http://localhost:10020/engine-rest|http://localhost:10020/outbox-rest"
    );

    Camunda7HealthContributor contributor = new Camunda7HealthContributor(
            mock(SystemConnectorHealthRegistry.class),
            mock(RestClient.class),
            properties,
            camundaUrls);

    assertThat(contributor.isEnabled()).isFalse();
  }
}
