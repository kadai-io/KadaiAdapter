package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.scheduler.SchedulerHealthComposite;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

class SchedulerHealthCompositeTest {

  private SchedulerHealthComposite schedulerHealthCompositeSpy;
  private LastSchedulerRun lastSchedulerRunSpy;

  @BeforeEach
  void setUp() {
    this.lastSchedulerRunSpy = Mockito.spy(new LastSchedulerRun());
    this.schedulerHealthCompositeSpy = Mockito.spy(new SchedulerHealthComposite(lastSchedulerRunSpy));
  }

  @Test
  void should_ReturnUp_When_SchedulerRuns() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(5));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(validRunTime);

    Health health =
        Health.up().withDetail("Last Run", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthCompositeSpy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(lastSchedulerRunSpy.getLastRunTime()).thenReturn(invalidRunTime);

    Health health =
        Health.down().withDetail("Last Run", lastSchedulerRunSpy.getLastRunTime()).build();

    assertThat(schedulerHealthCompositeSpy.health()).isEqualTo(health);
  }
}
