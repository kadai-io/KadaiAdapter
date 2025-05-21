package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

class SchedulerHealthIndicatorTest {

  private SchedulerHealthIndicator schedulerHealthIndicatorSpy;
  private LastSchedulerRun lastSchedulerRunSpy;

  @BeforeEach
  void setUp() {
    this.lastSchedulerRunSpy = Mockito.spy(new LastSchedulerRun());
    this.schedulerHealthIndicatorSpy =
        Mockito.spy(new SchedulerHealthIndicator(lastSchedulerRunSpy, Duration.ofMinutes(5)));
  }

  @Test
  void should_ReturnUp_When_SchedulerRuns() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(3));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(validRunTime);

    Health health = Health.up().withDetail("lastRun", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthIndicatorSpy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(invalidRunTime);

    Health health =
        Health.down().withDetail("lastRun", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthIndicatorSpy.health()).isEqualTo(health);
  }
}
