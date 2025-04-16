package io.kadai.adapter.configuration;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthIndicator;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerHealthIndicatorConfig {

  @Value("${kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds:5000}")
  private long taskStarterInterval;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds:5000}")
  private long referencedTaskCompleterInterval;

  @Value("${kadai.adapter.scheduler.run.interval.for.claim.referenced.tasks.in.milliseconds:5000}")
  private long referencedTaskClaimerInterval;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds:"
          + "5000}")
  private long referencedTaskClaimCancelerInterval;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds:"
          + "5000}")
  private long taskTerminatorInterval;

  @Bean
  public HealthIndicator kadaiTaskStarterSchedulerHealthCheck(
      @Qualifier("kadaiTaskStarterLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, Duration.ofMillis(taskStarterInterval * 2));
  }

  @Bean
  public HealthIndicator kadaiTaskTerminatorSchedulerHealthCheck(
      @Qualifier("kadaiTaskTerminatorLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, Duration.ofMillis(taskTerminatorInterval * 2));
  }

  @Bean
  public HealthIndicator referencedTaskClaimCancelerSchedulerHealthCheck(
      @Qualifier("referencedTaskClaimCancelerLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(
        lastRun, Duration.ofMillis(referencedTaskClaimCancelerInterval * 2));
  }

  @Bean
  public HealthIndicator referencedTaskClaimerSchedulerHealthCheck(
      @Qualifier("referencedTaskClaimerLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(
        lastRun, Duration.ofMillis(referencedTaskClaimerInterval * 2));
  }

  @Bean
  public HealthIndicator referencedTaskCompleterSchedulerHealthCheck(
      @Qualifier("referencedTaskCompleterLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(
        lastRun, Duration.ofMillis(referencedTaskCompleterInterval * 2));
  }
}
