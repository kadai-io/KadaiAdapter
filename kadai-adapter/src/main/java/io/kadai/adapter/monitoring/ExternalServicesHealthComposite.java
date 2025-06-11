package io.kadai.adapter.monitoring;

import io.kadai.adapter.configuration.health.ExternalServicesHealthConfigurationProperties;
import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.ReferencedTaskClaimer;
import io.kadai.adapter.impl.ReferencedTaskCompleter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
  private final List<String> camundaUrls = new ArrayList<>();
  private final List<String> outboxUrls = new ArrayList<>();

  @Autowired
  public ExternalServicesHealthComposite(
      ExternalServicesHealthConfigurationProperties properties,
      RestTemplate restTemplate,
      ReferencedTaskCompleter referencedTaskCompleter,
      ReferencedTaskClaimer referencedTaskClaimer,
      ReferencedTaskClaimCanceler referencedTaskClaimCanceler,
      KadaiTaskStarter kadaiTaskStarter,
      KadaiTaskTerminator kadaiTaskTerminator,
      @Value("${kadai-system-connector-camundaSystemURLs}") String urlCamundaSystem) {
    String[] entries = urlCamundaSystem.split(",");
    for (String entry : entries) {
      String[] parts = entry.split("\\|");
      if (parts.length == 2) {
        camundaUrls.add(parts[0].trim());
        outboxUrls.add(parts[1].trim());
      } else {
        throw new IllegalArgumentException("Invalid Camunda System URL format for: " + entry);
      }
    }
    if (properties.getCamunda().getEnabled()) {
      healthContributors.put("camunda", new CamundaHealthComposite(restTemplate, camundaUrls));
    }
    if (properties.getKadai().getEnabled()) {
      healthContributors.put("kadai", new KadaiHealthIndicator());
    }
    if (properties.getOutbox().getEnabled()) {
      healthContributors.put("outbox", new OutboxHealthComposite(restTemplate, outboxUrls));
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
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
