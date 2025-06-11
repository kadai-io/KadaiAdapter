package io.kadai.adapter.monitoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

public class CamundaHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public CamundaHealthComposite(RestTemplate restTemplate, List<String> camundaUrls) {
    int i = 1;
    for (String camundaUrl : camundaUrls) {
      healthContributors.put(
          "camundaSystemRest" + i, new CamundaHealthIndicator(restTemplate, camundaUrl));
      i++;
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
