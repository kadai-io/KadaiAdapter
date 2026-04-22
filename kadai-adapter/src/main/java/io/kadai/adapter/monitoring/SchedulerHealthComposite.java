package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import io.kadai.adapter.impl.scheduled.KadaiTaskCompletionOrchestrator;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimer;
import io.kadai.adapter.impl.scheduled.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;

public class SchedulerHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public SchedulerHealthComposite(
      SchedulerHealthConfigurationProperties properties,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarterOrchestrator kadaiTaskStarter,
      KadaiTaskCompletionOrchestrator kadaiTaskTerminator) {
    if (properties.getReferencedTaskCompleter().getEnabled()) {
      healthContributors.put(
          "referencedTaskCompleter",
          new SchedulerHealthIndicator(
              referencedTaskCompleter, properties.getRunTimeAcceptanceMultiplier()));
    }
    if (properties.getReferencedTaskClaimer().getEnabled()) {
      healthContributors.put(
          "referencedTaskClaimer",
          new SchedulerHealthIndicator(
              referencedTaskClaimer, properties.getRunTimeAcceptanceMultiplier()));
    }
    if (properties.getReferencedTaskClaimCanceler().getEnabled()) {
      healthContributors.put(
          "referencedTaskClaimCanceler",
          new SchedulerHealthIndicator(
              referencedTaskClaimCanceler, properties.getRunTimeAcceptanceMultiplier()));
    }
    if (properties.getKadaiTaskStarter().getEnabled()) {
      healthContributors.put(
          "kadaiTaskStarter",
          new SchedulerHealthIndicator(
              kadaiTaskStarter, properties.getRunTimeAcceptanceMultiplier()));
    }
    if (properties.getKadaiTaskTerminator().getEnabled()) {
      healthContributors.put(
          "kadaiTaskTerminator",
          new SchedulerHealthIndicator(
              kadaiTaskTerminator, properties.getRunTimeAcceptanceMultiplier()));
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  public Stream<Entry> stream() {
    return healthContributors.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue()));
  }
}
