package io.kadai.adapter.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;

/**
 * Health-Contributor for the entirety of <b>all</b> plugins.
 *
 * <p>This contributor includes all Health-Indicators for systems or components inherently bound to
 * or owned by <b>any</b> KadaiAdapter-Plugin.
 *
 * <p>Plugins itself may be {@link CompositeHealthContributor CompositeHealthContributors}.
 */
public class PluginHealth implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public PluginHealth(List<PluginHealthContributorFactory> pluginHealthContributorFactories) {
    for (PluginHealthContributorFactory factory : pluginHealthContributorFactories) {
      factory
          .newInstance()
          .ifPresent(contributor -> healthContributors.put(factory.getPluginName(), contributor));
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
