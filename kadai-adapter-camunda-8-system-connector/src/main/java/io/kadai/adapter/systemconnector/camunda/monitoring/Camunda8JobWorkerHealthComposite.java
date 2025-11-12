package io.kadai.adapter.systemconnector.camunda.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties.JobWorkerHealthConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.lang.NonNull;

public class Camunda8JobWorkerHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public Camunda8JobWorkerHealthComposite(
      JobWorkerHealthConfigurationProperties properties,
      UserTaskCompletion complete,
      UserTaskCreation create,
      UserTaskCancellation cancel) {
    if (properties.getComplete().getEnabled()) {
      healthContributors.put("complete", new Camunda8JobWorkerHealthIndicator(complete));
    }
    if (properties.getCreate().getEnabled()) {
      healthContributors.put("create", new Camunda8JobWorkerHealthIndicator(create));
    }
    if (properties.getCancel().getEnabled()) {
      healthContributors.put("cancel", new Camunda8JobWorkerHealthIndicator(cancel));
    }
  }

  @Override
  public HealthContributor getContributor(String name) {
    return healthContributors.get(name);
  }

  @Override
  @NonNull
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return healthContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
