package io.kadai.adapter.monitoring;

import io.kadai.adapter.impl.LastSchedulerRun;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("external-services")
@ConditionalOnEnabledHealthIndicator("external-services")
public class ExternalServicesHealthCheck implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new LinkedHashMap<>();

  @Autowired
  public ExternalServicesHealthCheck(
      ExternalServicesHealthConfigurationProperties properties,
      RestTemplate restTemplate,
      CamundaSystemConfigurationProperties camundaSystemConfigurationProperties,
      OutboxSystemConfigurationProperties outboxSystemConfigurationProperties,
      LastSchedulerRun lastSchedulerRun) {
    if (properties.getCamunda().getEnabled()) {
      healthContributors.put(
          "camunda",
          new CamundaHealthCheck(
              restTemplate,
              camundaSystemConfigurationProperties.getAddress(),
              camundaSystemConfigurationProperties.getPort(),
              camundaSystemConfigurationProperties.getContextPath(),
              camundaSystemConfigurationProperties.getEndpoint(),
              camundaSystemConfigurationProperties.getQuery()));
    }
    if (properties.getKadai().getEnabled()) {
      healthContributors.put("kadai", new KadaiHealthCheck());
    }
    if (properties.getOutbox().getEnabled()) {
      healthContributors.put(
          "outbox",
          new OutboxHealthCheck(
              restTemplate,
              outboxSystemConfigurationProperties.getAddress(),
              outboxSystemConfigurationProperties.getPort(),
              outboxSystemConfigurationProperties.getContextPath(),
              outboxSystemConfigurationProperties.getEndpoint(),
              outboxSystemConfigurationProperties.getQuery()));
    }
    if (properties.getScheduler().getEnabled()) {
      healthContributors.put("scheduler", new SchedulerHealthCheck(lastSchedulerRun));
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
        .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
