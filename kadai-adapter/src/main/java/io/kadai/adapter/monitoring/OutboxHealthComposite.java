package io.kadai.adapter.monitoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.web.client.RestTemplate;

public class OutboxHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public OutboxHealthComposite(RestTemplate restTemplate, List<String> outboxUrls) {
    int i = 1;
    for (String outboxUrl : outboxUrls) {
      healthContributors.put("outboxRest" + i, new OutboxHealthIndicator(restTemplate, outboxUrl));
      i++;
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
