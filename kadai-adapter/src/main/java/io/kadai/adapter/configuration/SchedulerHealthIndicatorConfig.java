package io.kadai.adapter.configuration;

import io.kadai.adapter.impl.LastSchedulerRun;
import io.kadai.adapter.monitoring.SchedulerHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerHealthIndicatorConfig {

  @Bean("kadaiTaskStarterSchedulerHealthCheck")
  public HealthIndicator kadaiTaskStarterSchedulerHealthCheck(
      @Qualifier("kadaiTaskStarterLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, "Kadai Task Starter");
  }

  @Bean("kadaiTaskTerminatorSchedulerHealthCheck")
  public HealthIndicator kadaiTaskTerminatorSchedulerHealthCheck(
      @Qualifier("kadaiTaskTerminatorLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, "Kadai Task Terminator");
  }

  @Bean("referencedTaskClaimCancelerSchedulerHealthCheck")
  public HealthIndicator referencedTaskClaimCancelerSchedulerHealthCheck(
      @Qualifier("referencedTaskClaimCancelerLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, "Referenced Task Claim Canceler");
  }

  @Bean("referencedTaskClaimerSchedulerHealthCheck")
  public HealthIndicator referencedTaskClaimerSchedulerHealthCheck(
      @Qualifier("referencedTaskClaimerLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, "Referenced Task Claimer");
  }

  @Bean("referencedTaskCompleterSchedulerHealthCheck")
  public HealthIndicator referencedTaskCompleterSchedulerHealthCheck(
      @Qualifier("referencedTaskCompleterLastRun") LastSchedulerRun lastRun) {
    return new SchedulerHealthIndicator(lastRun, "Referenced Task Completer");
  }
}
