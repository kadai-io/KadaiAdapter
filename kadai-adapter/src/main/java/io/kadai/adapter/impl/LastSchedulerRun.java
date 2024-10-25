package io.kadai.adapter.impl;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LastSchedulerRun {
  private Instant lastRunTime;

  @PostConstruct
  void init() {
    this.touch();
  }

  public Instant getLastRunTime() {
    return lastRunTime;
  }

  void touch() {
    lastRunTime = Instant.now();
  }
}
