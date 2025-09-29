package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class Camunda7HealthContributorTest {

  @Test
  void should_CreateCamundaSystemsHealthComposite_When_Enabled() {
    ExternalServicesHealthConfigurationProperties properties =
            new ExternalServicesHealthConfigurationProperties();

    List<String> camundaUrls = List.of(
            "http://localhost:10020/engine-rest|http://localhost:10020/outbox-rest"
    );

    Camunda7HealthContributor contributor = new Camunda7HealthContributor(
            mock(SystemConnectorHealthRegistry.class),
            mock(RestTemplate.class),
            properties,
            camundaUrls);

    assertThat(contributor.getConnectorName()).isEqualTo("camundaSystems");
    assertThat(contributor.createHealthContributor())
            .isInstanceOf(Camunda7SystemsHealthComposite.class);
    assertThat(contributor.isEnabled()).isTrue();
  }

  @Test
  void should_BeDisabled_When_CamundaSystemDisabled() {
    ExternalServicesHealthConfigurationProperties properties =
            new ExternalServicesHealthConfigurationProperties()
                    .withCamundaSystem(
                            (ExternalServicesHealthConfigurationProperties
                                    .CamundaSystemHealthConfigurationProperties)
                                        new ExternalServicesHealthConfigurationProperties
                                                .CamundaSystemHealthConfigurationProperties()
                                                .withEnabled(false)
                    );

    List<String> camundaUrls = List.of(
            "http://localhost:10020/engine-rest|http://localhost:10020/outbox-rest"
    );

    Camunda7HealthContributor contributor = new Camunda7HealthContributor(
            mock(SystemConnectorHealthRegistry.class),
            mock(RestTemplate.class),
            properties,
            camundaUrls);

    assertThat(contributor.isEnabled()).isFalse();
  }
}
