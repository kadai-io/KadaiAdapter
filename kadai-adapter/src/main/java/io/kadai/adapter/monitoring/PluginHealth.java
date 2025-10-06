package io.kadai.adapter.monitoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;

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

  @NonNull
  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
