package io.kadai.adapter.impl;

import java.time.Duration;

public interface ScheduledComponent {

  LastSchedulerRun getLastSchedulerRun();

  Duration getRunInterval();
}
