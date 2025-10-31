package io.kadai.adapter.impl.scheduled;

import io.kadai.adapter.monitoring.MonitoredComponent;
import java.time.Duration;

/**
 * A component that is scheduled, e.g. with {@linkplain
 * org.springframework.scheduling.annotation.Scheduled @Scheduled}.
 */
public interface MonitoredScheduledComponent extends MonitoredComponent {

  /**
   * Returns the duration of the interval this scheduled component runs.
   *
   * @return run interval
   */
  Duration getRunInterval();
}
