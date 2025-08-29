package io.kadai.adapter.impl.scheduled;

import java.time.Instant;

public class SchedulerRun {

  private Instant runTime;

  public SchedulerRun() {
    this.touch();
  }

  public Instant getRunTime() {
    return runTime;
  }

  void touch() {
    runTime = Instant.now();
  }
}
