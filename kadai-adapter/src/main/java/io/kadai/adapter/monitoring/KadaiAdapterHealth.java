package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.KernelHealthConfigurationProperties;
import io.kadai.adapter.configuration.health.PluginHealthConfigurationProperties;
import io.kadai.adapter.impl.scheduled.KadaiTaskCompletionOrchestrator;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.scheduled.ReferencedTaskClaimer;
import io.kadai.adapter.impl.scheduled.ReferencedTaskCompleter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
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

  @Override
  public Stream<Entry> stream() {
    return healthContributors.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue()));
  }
}
