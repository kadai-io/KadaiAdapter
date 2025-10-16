package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.monitoring.MonitoredComponent;
import io.kadai.adapter.monitoring.MonitoredRun;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

public class Camunda8JobWorkerHealthIndicatorTest {

  @Test
  void should_ReturnUp_When_MonitoredRunSucceeded() {
    final MonitoredComponent monitoredComponent =
        new MonitoredComponent() {
          @Override
          public MonitoredRun getLastRun() {
            final MonitoredRun monitoredRun = new MonitoredRun();
            monitoredRun.start();
            monitoredRun.succeed();
            return monitoredRun;
          }

          @Override
          public Duration getExpectedRunDuration() {
            return Duration.ofMillis(42);
          }
        };

    final Health actual = new Camunda8JobWorkerHealthIndicator(monitoredComponent).health();

    assertThat(actual.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_MonitoredRunFailed() {
    final MonitoredComponent monitoredComponent =
        new MonitoredComponent() {
          @Override
          public MonitoredRun getLastRun() {
            final MonitoredRun monitoredRun = new MonitoredRun();
            monitoredRun.start();
            monitoredRun.fail();
            return monitoredRun;
          }

          @Override
          public Duration getExpectedRunDuration() {
            return Duration.ofMillis(42);
          }
        };

    final Health actual = new Camunda8JobWorkerHealthIndicator(monitoredComponent).health();

    assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnUnknown_When_MonitoredRunEndIsNull() {
    final MonitoredComponent monitoredComponent =
        new MonitoredComponent() {
          @Override
          public MonitoredRun getLastRun() {
            final MonitoredRun monitoredRun = new MonitoredRun();
            monitoredRun.start();
            return monitoredRun;
          }

          @Override
          public Duration getExpectedRunDuration() {
            return Duration.ofMillis(42);
          }
        };

    final Health actual = new Camunda8JobWorkerHealthIndicator(monitoredComponent).health();

    assertThat(actual.getStatus()).isEqualTo(Status.UNKNOWN);
  }
}
