package io.kadai.adapter.integration;

import static io.kadai.adapter.integration.HealthCheckEndpoints.HEALTH_ENDPOINT;
import static io.kadai.adapter.integration.SchedulerHealthCheckTest.TestConfig.DUMMY_RUN_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import io.kadai.adapter.monitoring.SchedulerHealthIndicator;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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
    public static final Instant DUMMY_RUN_TIME = Instant.now().minus(Duration.ofMinutes(5));

    @Bean
    SchedulerHealthCheck schedulerHealthCheck() {
      LastSchedulerRun starterLastRun = mock(LastSchedulerRun.class);
      LastSchedulerRun terminatorLastRun = mock(LastSchedulerRun.class);
      LastSchedulerRun claimerLastRun = mock(LastSchedulerRun.class);
      LastSchedulerRun completerLastRun = mock(LastSchedulerRun.class);
      LastSchedulerRun cancelerLastRun = mock(LastSchedulerRun.class);

      when(starterLastRun.getLastRunTime()).thenReturn(DUMMY_RUN_TIME);
      when(terminatorLastRun.getLastRunTime()).thenReturn(DUMMY_RUN_TIME);
      when(claimerLastRun.getLastRunTime()).thenReturn(DUMMY_RUN_TIME);
      when(completerLastRun.getLastRunTime()).thenReturn(DUMMY_RUN_TIME);
      when(cancelerLastRun.getLastRunTime()).thenReturn(DUMMY_RUN_TIME);

      HealthIndicator starter =
          spy(new SchedulerHealthIndicator(starterLastRun, Duration.ofMinutes(10)));
      HealthIndicator terminator =
          spy(new SchedulerHealthIndicator(terminatorLastRun, Duration.ofMinutes(10)));
      HealthIndicator claimer =
          spy(new SchedulerHealthIndicator(claimerLastRun, Duration.ofMinutes(10)));
      HealthIndicator completer =
          spy(new SchedulerHealthIndicator(completerLastRun, Duration.ofMinutes(10)));
      HealthIndicator canceler =
          spy(new SchedulerHealthIndicator(cancelerLastRun, Duration.ofMinutes(10)));

      SchedulerHealthCheck composite =
          new SchedulerHealthCheck(starter, terminator, canceler, claimer, completer);
      return spy(composite);
    }
  }

  @Autowired private SchedulerHealthCheck schedulerHealthIndicator;

  @Autowired private TestRestTemplate testRestTemplate;

  private SchedulerHealthCheck spyHealthCheck;

  @BeforeEach
  void setUp() {
    spyHealthCheck = schedulerHealthIndicator;
  }

  static Stream<String> schedulerNames() {
    return Stream.of(
        "Kadai Task Starter",
        "Kadai Task Terminator",
        "Referenced Task Claimer",
        "Referenced Task Completer",
        "Referenced Task Claim Canceler");
  }

  @ParameterizedTest(name = "{0} should be UP")
  @MethodSource("schedulerNames")
  void should_ReturnUp_When_SchedulerRuns(String name) {
    Instant recentRun = Instant.now().minus(Duration.ofMillis(1000));
    LastSchedulerRun mockRun = mock(LastSchedulerRun.class);
    when(mockRun.getLastRunTime()).thenReturn(recentRun);

    HealthIndicator indicator = new SchedulerHealthIndicator(mockRun, Duration.ofMillis(1500));
    Health expected = Health.up().withDetail("Last Run", recentRun).build();

    assertThat(indicator.health()).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0} should be DOWN")
  @MethodSource("schedulerNames")
  void should_ReturnDown_When_SchedulerRuns(String name) {
    Instant recentRun = Instant.now().minus(Duration.ofMillis(2000));
    LastSchedulerRun mockRun = mock(LastSchedulerRun.class);
    when(mockRun.getLastRunTime()).thenReturn(recentRun);

    HealthIndicator indicator = new SchedulerHealthIndicator(mockRun, Duration.ofMillis(1500));
    Health expected = Health.down().withDetail("Last Run", recentRun).build();

    assertThat(indicator.health()).isEqualTo(expected);
  }

  @Test
  void should_ReturnUp_When_CallingHealthEndpoint() {
    HealthIndicator starterHealthCheck =
        (HealthIndicator) schedulerHealthIndicator.getContributor("Kadai Task Starter");
    when(starterHealthCheck.health())
        .thenReturn(Health.up().withDetail("Last Run", DUMMY_RUN_TIME).build());
    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Last Run", DUMMY_RUN_TIME.toString());
  }

  @Test
  void should_ReturnDown_When_CallingHealthEndpoint() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    HealthIndicator starterHealthCheck =
        (HealthIndicator) schedulerHealthIndicator.getContributor("Kadai Task Starter");
    when(starterHealthCheck.health())
        .thenReturn(Health.down().withDetail("Last Run", invalidRunTime).build());

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).contains("Last Run", invalidRunTime.toString());
  }
}
