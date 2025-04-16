package io.kadai.adapter.impl;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

public class LastSchedulerRun {
  private Instant lastRunTime;

  @PostConstruct
  void init() {
    this.touch();
  }

  public Instant getLastRunTime() {
    return lastRunTime;
  }

  public void touch() {
    lastRunTime = Instant.now();
  }
}
