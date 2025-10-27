package io.kadai.adapter.monitoring;

import java.util.Optional;
import org.springframework.boot.actuate.health.HealthContributor;

/**
 * Instantiated factory for KadaiAdapter-specific plugin-health-contributors.
 *
 * <p>Implement this interface for any contributing Health-Indicators.
 *
 * <p>Implementations of this interface will be picked up by {@link KadaiAdapterHealth} via
 * <b>classpath-scanning</b> in a SpringBoot-Application. Therefore, you <b>need to</b> lift your
 * implementation into the Spring-Context, e.g. by annotating it as {@code @Component}.
 */
public interface PluginHealthContributorFactory {

  /**
   * Returns the name of the plugin.
   *
   * <p>This name will be used as an <b>identifier</b> for Health-Contributors created by
   * <i>this</i> factory. It will further be used as JSON-Field in the actuators' health-response.
   *
   * @return name of the plugin
   */
  String getPluginName();

  /**
   * Returns an optional instance of the Health-Contributor created by <i>this factory</i>.
   *
   * <p>It may return {@link Optional#empty()}, e.g. to control activation of this plugin through
   * extra properties.
   *
   * @return an optional instance of the Health-Contributor
   */
  Optional<HealthContributor> newInstance();
}
