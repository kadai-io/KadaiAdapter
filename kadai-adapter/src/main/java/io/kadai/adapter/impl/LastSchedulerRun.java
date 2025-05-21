package io.kadai.adapter.impl;

import java.time.Instant;

public class LastSchedulerRun {

  private Instant lastRunTime;

  public LastSchedulerRun() {
    this.touch();
  }

  public Instant getLastRunTime() {
    return lastRunTime;
  }

  void touch() {
    lastRunTime = Instant.now();
  }
}
