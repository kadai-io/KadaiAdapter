package io.kadai.adapter.impl;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

/** A bean template for tracking the last run of a specific type of job. */
public class LastSchedulerRun {
  private Instant lastRunTime;

  @PostConstruct
  void init() {
    this.touch();
  }

  public Instant getLastRunTime() {
    return lastRunTime;
  }

  /** To be called at the end of the scheduled job to update the timestamp of last run. */
  void touch() {
    lastRunTime = Instant.now();
  }
}
