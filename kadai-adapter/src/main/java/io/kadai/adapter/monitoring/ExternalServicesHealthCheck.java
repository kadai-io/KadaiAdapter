package io.kadai.adapter.monitoring;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.stereotype.Component;

@Component("external-services")
public class ExternalServicesHealthCheck implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new LinkedHashMap<>();

  @Autowired
  public ExternalServicesHealthCheck(
      CamundaHealthCheck camundaHealthIndicator,
      OutboxHealthCheck outboxHealthIndicator,
      KadaiHealthCheck kadaiHealthIndicator,
      SchedulerHealthCheck schedulerHealthCheck) {

    healthContributors.put("Camunda Health", camundaHealthIndicator);
    healthContributors.put("Outbox Health", outboxHealthIndicator);
    healthContributors.put("Kadai Health", kadaiHealthIndicator);
    healthContributors.put("Scheduler Health", schedulerHealthCheck);
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
