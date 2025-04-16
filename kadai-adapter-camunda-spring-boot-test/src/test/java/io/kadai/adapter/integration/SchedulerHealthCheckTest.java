package io.kadai.adapter.integration;

import static io.kadai.adapter.integration.HealthCheckEndpoints.HEALTH_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.KadaiTaskStarterSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.KadaiTaskTerminatorSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskClaimCancelerSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskClaimerSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskCompleterSchedulerHealthCheck;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.internal.util.Pair;
import io.kadai.common.test.security.JaasExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
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
    static final Instant DUMMY_RUN_TIME = Instant.now().minus(Duration.ofMinutes(5));

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

      KadaiTaskStarterSchedulerHealthCheck starter =
          spy(new KadaiTaskStarterSchedulerHealthCheck(starterLastRun));
      KadaiTaskTerminatorSchedulerHealthCheck terminator =
          spy(new KadaiTaskTerminatorSchedulerHealthCheck(terminatorLastRun));
      ReferencedTaskClaimerSchedulerHealthCheck claimer =
          spy(new ReferencedTaskClaimerSchedulerHealthCheck(claimerLastRun));
      ReferencedTaskCompleterSchedulerHealthCheck completer =
          spy(new ReferencedTaskCompleterSchedulerHealthCheck(completerLastRun));
      ReferencedTaskClaimCancelerSchedulerHealthCheck canceler =
          spy(new ReferencedTaskClaimCancelerSchedulerHealthCheck(cancelerLastRun));

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

  @TestFactory
  Stream<DynamicTest> should_ReturnUp_When_SchedulerRuns() throws Exception {
    Stream<Pair<String, Class<? extends HealthIndicator>>> input =
        Stream.of(
            Pair.of("Kadai Task Starter", KadaiTaskStarterSchedulerHealthCheck.class),
            Pair.of("Kadai Task Terminator", KadaiTaskTerminatorSchedulerHealthCheck.class),
            Pair.of("Referenced Task Claimer", ReferencedTaskClaimerSchedulerHealthCheck.class),
            Pair.of("Referenced Task Completer", ReferencedTaskCompleterSchedulerHealthCheck.class),
            Pair.of(
                "Referenced Task Claim Canceler",
                ReferencedTaskClaimCancelerSchedulerHealthCheck.class));

    ThrowingConsumer<Pair<String, Class<? extends HealthIndicator>>> test =
        t -> {
          Instant recentRun = Instant.now().minus(Duration.ofMinutes(5));
          LastSchedulerRun mockRun = mock(LastSchedulerRun.class);
          when(mockRun.getLastRunTime()).thenReturn(recentRun);

          HealthIndicator indicator =
              t.getRight().getConstructor(LastSchedulerRun.class).newInstance(mockRun);
          Health expected = Health.up().withDetail("Last Run", recentRun).build();

          assertThat(indicator.health()).isEqualTo(expected);
        };
    return DynamicTest.stream(input, Pair::getLeft, test);
  }

  @TestFactory
  Stream<DynamicTest> should_ReturnDown_When_SchedulerRuns() throws Exception {
    Stream<Pair<String, Class<? extends HealthIndicator>>> input =
        Stream.of(
            Pair.of("Kadai Task Starter", KadaiTaskStarterSchedulerHealthCheck.class),
            Pair.of("Kadai Task Terminator", KadaiTaskTerminatorSchedulerHealthCheck.class),
            Pair.of("Referenced Task Claimer", ReferencedTaskClaimerSchedulerHealthCheck.class),
            Pair.of("Referenced Task Completer", ReferencedTaskCompleterSchedulerHealthCheck.class),
            Pair.of(
                "Referenced Task Claim Canceler",
                ReferencedTaskClaimCancelerSchedulerHealthCheck.class));

    ThrowingConsumer<Pair<String, Class<? extends HealthIndicator>>> test =
        t -> {
          Instant recentRun = Instant.now().minus(Duration.ofMinutes(15));
          LastSchedulerRun mockRun = mock(LastSchedulerRun.class);
          when(mockRun.getLastRunTime()).thenReturn(recentRun);

          HealthIndicator indicator =
              t.getRight().getConstructor(LastSchedulerRun.class).newInstance(mockRun);
          Health expected = Health.down().withDetail("Last Run", recentRun).build();

          assertThat(indicator.health()).isEqualTo(expected);
        };
    return DynamicTest.stream(input, Pair::getLeft, test);
  }

  @Test
  void should_ReturnUp_When_CallingHealthEndpoint() {
    HealthIndicator starterHealthCheck =
        (HealthIndicator) schedulerHealthIndicator.getContributor("Kadai Task Starter");
    when(starterHealthCheck.health())
        .thenReturn(Health.up().withDetail("Last Run", TestConfig.DUMMY_RUN_TIME).build());
    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Last Run", TestConfig.DUMMY_RUN_TIME.toString());
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
