package io.kadai.adapter.monitoring;

import java.time.Duration;
import java.time.Instant;

public class MonitoredRun {

  private Instant start;
  private Instant end;
  private Boolean successful;

  public Instant getStart() {
    return start;
  }

  public Instant getEnd() {
    return end;
  }

  public Boolean isSuccessful() {
    return successful;
  }

  public void start() {
    this.start = Instant.now();
  }

  public void succeed() {
    this.end = Instant.now();
    this.successful = true;
  }

  public void fail() {
    this.end = Instant.now();
    this.successful = false;
  }

  public Duration getDuration() {
    return Duration.between(start, end);
  }
}
