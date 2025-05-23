package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties.SchedulerHealthConfigurationProperties;
import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.ReferencedTaskClaimer;
import io.kadai.adapter.impl.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;

public class SchedulerHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public SchedulerHealthComposite(
      SchedulerHealthConfigurationProperties properties,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarter kadaiTaskStarter,
      KadaiTaskTerminator kadaiTaskTerminator) {
    if (properties.getReferencedTaskCompleter().getEnabled()) {
      healthContributors.put(
          "referencedTaskCompleter", new SchedulerHealthIndicator(referencedTaskCompleter));
    }
    if (properties.getReferencedTaskClaimer().getEnabled()) {
      healthContributors.put(
          "referencedTaskClaimer", new SchedulerHealthIndicator(referencedTaskClaimer));
    }
    if (properties.getReferencedTaskClaimCanceler().getEnabled()) {
      healthContributors.put(
          "referencedTaskClaimCanceler", new SchedulerHealthIndicator(referencedTaskClaimCanceler));
    }
    if (properties.getKadaiTaskStarter().getEnabled()) {
      healthContributors.put("kadaiTaskStarter", new SchedulerHealthIndicator(kadaiTaskStarter));
    }
    if (properties.getKadaiTaskTerminator().getEnabled()) {
      healthContributors.put(
          "kadaiTaskTerminator", new SchedulerHealthIndicator(kadaiTaskTerminator));
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
