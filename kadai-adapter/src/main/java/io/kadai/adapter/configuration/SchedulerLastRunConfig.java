package io.kadai.adapter.configuration;

import io.kadai.adapter.impl.LastSchedulerRun;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerLastRunConfig {
  @Bean(name = "kadaiTaskStarterLastRun")
  public LastSchedulerRun kadaiTaskStarterLastRun() {
    return new LastSchedulerRun();
  }

  @Bean(name = "kadaiTaskTerminatorLastRun")
  public LastSchedulerRun kadaiTaskTerminatorLastRun() {
    return new LastSchedulerRun();
  }

  @Bean(name = "referencedTaskClaimCancelerLastRun")
  public LastSchedulerRun referencedTaskClaimCancelerLastRun() {
    return new LastSchedulerRun();
  }

  @Bean(name = "referencedTaskClaimerLastRun")
  public LastSchedulerRun referencedTaskClaimerLastRun() {
    return new LastSchedulerRun();
  }

  @Bean(name = "referencedTaskCompleterLastRun")
  public LastSchedulerRun referencedTaskCompleterLastRun() {
    return new LastSchedulerRun();
  }
}
