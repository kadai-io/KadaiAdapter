package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

class SchedulerHealthCheckTest {

  private SchedulerHealthCheck schedulerHealthCheckSpy;
  private LastSchedulerRun lastSchedulerRunSpy;

  @BeforeEach
  void setUp() {
    this.lastSchedulerRunSpy = Mockito.spy(new LastSchedulerRun());
    this.schedulerHealthCheckSpy = Mockito.spy(new SchedulerHealthCheck(lastSchedulerRunSpy));
  }

  @Test
  void should_ReturnUp_When_SchedulerRuns() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(5));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(validRunTime);

    Health health =
        Health.up().withDetail("Last Run", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthCheckSpy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(invalidRunTime);

    Health health =
        Health.down().withDetail("Last Run", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthCheckSpy.health()).isEqualTo(health);
  }
}
