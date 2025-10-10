package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.impl.scheduled.KadaiTaskCompletionOrchestrator;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimer;
import io.kadai.adapter.impl.scheduled.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;

/**
 * Health-Contributor for the entirety of the kernel.
 *
 * <p>This contributor includes all Health-Indicators for systems or components inherently bound to
 * or owned by the KadaiAdapter-Kernel.
 */
public class KernelHealth implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public KernelHealth(
      KernelHealthConfigurationProperties properties,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarterOrchestrator kadaiTaskStarter,
      KadaiTaskCompletionOrchestrator kadaiTaskTerminator) {

    if (properties.getKadai().getEnabled()) {
      healthContributors.put("kadai", new KadaiHealthIndicator());
    }
    if (properties.getScheduler().getEnabled()) {
      healthContributors.put(
          "scheduler",
          new SchedulerHealthComposite(
              properties.getScheduler(),
              referencedTaskCompleter,
              referencedTaskClaimer,
              referencedTaskClaimCanceler,
              kadaiTaskStarter,
              kadaiTaskTerminator));
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @NonNull
  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
