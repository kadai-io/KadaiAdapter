package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.ReferencedTaskClaimer;
import io.kadai.adapter.impl.ReferencedTaskCompleter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("externalServices")
@ConditionalOnEnabledHealthIndicator("external-services")
public class ExternalServicesHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  @Autowired
  public ExternalServicesHealthComposite(
      ExternalServicesHealthConfigurationProperties properties,
      RestTemplate restTemplate,
      @Value("${camundaOutboxService.address:http://localhost}") String camundaOutboxAddress,
      @Value("${camundaOutboxService.port:8090}") int camundaOutboxPort,
      @Value("${outbox.context-path:}") String contextPath,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarter kadaiTaskStarter,
      KadaiTaskTerminator kadaiTaskTerminator) {
    if (properties.getCamunda().getEnabled()) {
      healthContributors.put(
          "camunda",
          new CamundaHealthIndicator(
              restTemplate, camundaOutboxAddress, camundaOutboxPort, contextPath));
    }
    if (properties.getKadai().getEnabled()) {
      healthContributors.put("kadai", new KadaiHealthIndicator());
    }
    if (properties.getOutbox().getEnabled()) {
      healthContributors.put(
          "outbox",
          new OutboxHealthIndicator(
              restTemplate, camundaOutboxAddress, camundaOutboxPort, contextPath));
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
        .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
