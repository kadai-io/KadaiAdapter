package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.impl.scheduled.KadaiTaskCompletionOrchestrator;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimer;
import io.kadai.adapter.impl.scheduled.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component("externalServices")
@ConditionalOnEnabledHealthIndicator("external-services")
public class ExternalServicesHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();
  private final SystemConnectorHealthRegistry registry;
  private boolean systemConnectorsInitialized = false;

  @Autowired
  public ExternalServicesHealthComposite(
      ExternalServicesHealthConfigurationProperties properties,
      SystemConnectorHealthRegistry registry,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarterOrchestrator kadaiTaskStarter,
      KadaiTaskCompletionOrchestrator kadaiTaskTerminator) {

    this.registry = registry;

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

  private synchronized void initializeSystemConnectors() {
    if (!systemConnectorsInitialized) {
      Map<String, HealthContributor> connectors = registry.getEnabledHealthContributors();
      healthContributors.putAll(connectors);
      systemConnectorsInitialized = true;
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    initializeSystemConnectors();
    return healthContributors.get(name);
  }

  @NonNull
  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    initializeSystemConnectors();
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
