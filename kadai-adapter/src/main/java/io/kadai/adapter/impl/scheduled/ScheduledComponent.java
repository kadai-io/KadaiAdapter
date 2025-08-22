package io.kadai.adapter.impl.scheduled;

import java.time.Duration;

/**
 * A component that is scheduled, e.g. with {@linkplain
 * org.springframework.scheduling.annotation.Scheduled @Scheduled}.
 */
public interface ScheduledComponent {

  /**
   * Returns the last reported run of the scheduled component.
   *
   * @return last actual run
   */
  SchedulerRun getLastSchedulerRun();

  /**
   * Returns the duration of the interval this scheduled component runs.
   *
   * @return run interval
   */
  Duration getRunInterval();

  /**
   * Returns the expected duration one run of this scheduled component takes to complete.
   *
   * @return expected run duration
   */
  Duration getExpectedRunDuration();
}
