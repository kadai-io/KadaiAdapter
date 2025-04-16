package io.kadai.adapter.integration;

import static io.kadai.adapter.integration.HealthCheckEndpoints.HEALTH_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.KadaiHealthCheck;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class KadaiHealthCheckTest extends AbsIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    KadaiHealthCheck kadaiHealthCheck() throws Exception {
      KadaiHealthCheck realHealthCheck = new KadaiHealthCheck();
      KadaiHealthCheck spyHealthCheck = Mockito.spy(realHealthCheck);

      return spyHealthCheck;
    }
  }

  @Autowired private SchedulerHealthCheck schedulerHealthIndicator;

  @Autowired private KadaiHealthCheck kadaiHealthIndicator;

  @Autowired private TestRestTemplate testRestTemplate;

  private KadaiHealthCheck spyHealthCheck;

  @BeforeEach
  void setUp() {
    spyHealthCheck = kadaiHealthIndicator;
  }

  @Test
  void should_ReturnUp_When_ReturnValidSchemaVersion() throws Exception {
    KadaiHealthCheck kadaiHealthCheck = new KadaiHealthCheck();
    KadaiHealthCheck spy = Mockito.spy(kadaiHealthCheck);

    when(spy.getCurrentSchemaVersion()).thenReturn("1.0.0");

    Health health = Health.up().withDetail("Kadai Version", "1.0.0").build();
    assertThat(spy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_ExceptionThrownForSchemaVersion() throws Exception {
    KadaiHealthCheck kadaiHealthCheck = new KadaiHealthCheck();
    KadaiHealthCheck spy = Mockito.spy(kadaiHealthCheck);

    Mockito.doThrow(new RuntimeException("Simulated exception"))
        .when(spy)
        .getCurrentSchemaVersion();

    Health health = Health.down().withDetail("Kadai Service Error", "Simulated exception").build();
    assertThat(spy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnUp_When_CallingHealthEndpoint() {
    when(spyHealthCheck.health())
        .thenReturn(Health.up().withDetail("Kadai Version", "1.0.0").build());

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Kadai Version", "1.0.0");
  }

  @Test
  void should_ReturnDown_When_CallingHealthEndpoint() {
    when(spyHealthCheck.health())
        .thenReturn(Health.down().withDetail("Kadai Service Error", "Simulated failure").build());

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).contains("Kadai Service Error", "Simulated failure");
  }
}
