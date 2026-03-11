package io.kadai.adapter.systemconnector.camunda.monitoring;

import io.kadai.adapter.systemconnector.camunda.config.health.Camunda8HealthConfigurationProperties;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;

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
  public Stream<Entry> stream() {
    return healthContributors.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue()));
  }
}
