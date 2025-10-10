package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.PluginHealthConfigurationProperties;
import io.kadai.adapter.impl.scheduled.KadaiTaskCompletionOrchestrator;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimer;
import io.kadai.adapter.impl.scheduled.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/** Root Health-Contributor for all Kadai-Adapter components. */
@Component("kadaiAdapter")
@ConditionalOnEnabledHealthIndicator("kadai-adapter")
public class KadaiAdapterHealth implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  @Autowired
  public KadaiAdapterHealth(
      KernelHealthConfigurationProperties kernelHealthConfigurationProperties,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarterOrchestrator kadaiTaskStarter,
      KadaiTaskCompletionOrchestrator kadaiTaskTerminator,
      PluginHealthConfigurationProperties pluginHealthConfigurationProperties,
      List<PluginHealthContributorFactory> pluginHealthContributorFactories) {
    if (kernelHealthConfigurationProperties.getEnabled()) {
      healthContributors.put(
          "kernel",
          new KernelHealth(
              kernelHealthConfigurationProperties,
              referencedTaskCompleter,
              referencedTaskClaimer,
              referencedTaskClaimCanceler,
              kadaiTaskStarter,
              kadaiTaskTerminator));
    }
    if (pluginHealthConfigurationProperties.getEnabled()) {
      healthContributors.put("plugin", new PluginHealth(pluginHealthContributorFactories));
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
