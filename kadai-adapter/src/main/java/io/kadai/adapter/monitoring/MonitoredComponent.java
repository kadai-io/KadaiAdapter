package io.kadai.adapter.monitoring;

import java.time.Duration;

public interface MonitoredComponent {
  /**
   * Returns the last reported run of the monitored component.
   *
   * @return last actual run
   */
  MonitoredRun getLastRun();

  /**
   * Returns the expected duration one run of this monitored component takes to complete.
   *
   * @return expected run duration
   */
  Duration getExpectedRunDuration();
}
