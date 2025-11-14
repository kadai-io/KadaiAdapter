package io.kadai.adapter.systemconnector.camunda.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties;
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

public class Camunda8SystemHealthComposite implements CompositeHealthContributor {

  private final Map<String, HealthContributor> healthContributors = new HashMap<>();

  public Camunda8SystemHealthComposite(
      Camunda8HealthConfigurationProperties properties,
      UserTaskCompletion userTaskCompletion,
      UserTaskCreation userTaskCreation,
      UserTaskCancellation userTaskCancellation) {
    if (properties.getJobWorker().getEnabled()) {
      healthContributors.put(
          "jobWorker",
          new Camunda8JobWorkerHealthComposite(
              properties.getJobWorker(),
              userTaskCompletion,
              userTaskCreation,
              userTaskCancellation));
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
