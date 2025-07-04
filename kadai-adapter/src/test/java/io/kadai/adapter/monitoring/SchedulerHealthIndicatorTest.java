package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.ScheduledComponent;
import io.kadai.adapter.impl.SchedulerRun;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;

class SchedulerHealthIndicatorTest {

  private SchedulerHealthIndicator schedulerHealthIndicatorSpy;
  private SchedulerRun schedulerRunSpy;

  @BeforeEach
  void setUp() {
    this.schedulerRunSpy = Mockito.spy(new SchedulerRun());
    this.schedulerHealthIndicatorSpy =
        Mockito.spy(
            new SchedulerHealthIndicator(
                new ScheduledComponent() {

                  @Override
                  public SchedulerRun getLastSchedulerRun() {
                    return schedulerRunSpy;
                  }

                  @Override
                  public Duration getRunInterval() {
                    return Duration.ofMinutes(5);
                  }

                  @Override
                  public Duration getExpectedRunDuration() {
                    return Duration.ofMinutes(1);
                  }
                },
                2L));
  }

  @Test
  void should_ReturnUp_When_SchedulerRuns() {
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(3));
    when(schedulerRunSpy.getRunTime()).thenReturn(validRunTime);

    assertThat(schedulerHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(schedulerRunSpy.getRunTime()).thenReturn(invalidRunTime);

    assertThat(schedulerHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }
}
