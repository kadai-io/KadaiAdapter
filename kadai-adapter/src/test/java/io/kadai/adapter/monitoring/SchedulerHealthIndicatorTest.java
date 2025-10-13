package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.impl.scheduled.MonitoredScheduledComponent;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;

class SchedulerHealthIndicatorTest {

  private SchedulerHealthIndicator schedulerHealthIndicatorSpy;
  private MonitoredRun monitoredRunSpy;

  @BeforeEach
  void setUp() {
    this.monitoredRunSpy = Mockito.spy(new MonitoredRun());
    this.schedulerHealthIndicatorSpy =
        Mockito.spy(
            new SchedulerHealthIndicator(
                new MonitoredScheduledComponent() {

                  @Override
                  public MonitoredRun getLastRun() {
                    return monitoredRunSpy;
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
    when(monitoredRunSpy.getEnd()).thenReturn(validRunTime);

    assertThat(schedulerHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_SchedulerDoesNotRun() {
    Instant invalidRunTime = Instant.now().minus(Duration.ofMinutes(15));
    when(monitoredRunSpy.getEnd()).thenReturn(invalidRunTime);

    assertThat(schedulerHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }
}
