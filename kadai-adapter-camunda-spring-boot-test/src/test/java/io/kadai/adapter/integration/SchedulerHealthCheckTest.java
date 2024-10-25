package io.kadai.adapter.integration;

import static io.kadai.adapter.integration.HealthCheckEndpoints.HEALTH_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import java.time.Duration;
import java.time.Instant;
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
class SchedulerHealthCheckTest extends AbsIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    SchedulerHealthCheck schedulerHealthCheck() throws Exception {
      LastSchedulerRun lastSchedulerRunMock = Mockito.mock(LastSchedulerRun.class);
      Instant dummyRunTime = Instant.now().minus(Duration.ofMinutes(10));
      when(lastSchedulerRunMock.getLastRunTime()).thenReturn(dummyRunTime);

      SchedulerHealthCheck realHealthCheck = new SchedulerHealthCheck(lastSchedulerRunMock);
      SchedulerHealthCheck spyHealthCheck = Mockito.spy(realHealthCheck);

      return spyHealthCheck;
    }
  }

  @Autowired private SchedulerHealthCheck schedulerHealthIndicator;

  @Autowired private TestRestTemplate testRestTemplate;

  private SchedulerHealthCheck spyHealthCheck;

  @BeforeEach
  void setUp() {
    spyHealthCheck = schedulerHealthIndicator;
  }

  @Test
  void should_ReturnUp_When_SchedulerRuns() throws Exception {
    LastSchedulerRun spy = Mockito.spy(new LastSchedulerRun());
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(5));
    when(spy.getLastRunTime()).thenReturn(validRunTime);

    SchedulerHealthCheck schedulerHealthCheck = new SchedulerHealthCheck(spy);
    Health health = Health.up().withDetail("Last Run", spy.getLastRunTime()).build();

    assertThat(schedulerHealthCheck.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    LastSchedulerRun spy = Mockito.spy(new LastSchedulerRun());
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(spy.getLastRunTime()).thenReturn(invalidRunTime);

    SchedulerHealthCheck schedulerHealthCheck = new SchedulerHealthCheck(spy);
    Health health = Health.down().withDetail("Last Run", spy.getLastRunTime()).build();

    assertThat(schedulerHealthCheck.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnUp_When_CallingHealthEndpoint() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(5));
    when(spyHealthCheck.health())
        .thenReturn(Health.up().withDetail("Last Run", validRunTime).build());

    ResponseEntity<String> response =
        testRestTemplate.getForEntity(
            HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Last Run", validRunTime.toString());
  }

  @Test
  void should_ReturnDown_When_CallingHealthEndpoint() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(spyHealthCheck.health())
        .thenReturn(Health.down().withDetail("Last Run", validRunTime).build());

    ResponseEntity<String> response =
        testRestTemplate.getForEntity(
            HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).contains("Last Run", validRunTime.toString());
  }
}
