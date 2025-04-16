package io.kadai.adapter.monitoring;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.stereotype.Component;

@Component
public class SchedulerHealthCheck implements CompositeHealthContributor {

  private final Map<String, HealthContributor> schedulerContributors = new LinkedHashMap<>();

  @Autowired
  public SchedulerHealthCheck(
      @Qualifier("kadaiTaskStarterSchedulerHealthCheck")
          HealthIndicator kadaiTaskStarterSchedulerHealthCheck,
      @Qualifier("kadaiTaskTerminatorSchedulerHealthCheck")
          HealthIndicator kadaiTaskTerminatorSchedulerHealthCheck,
      @Qualifier("referencedTaskClaimCancelerSchedulerHealthCheck")
          HealthIndicator referencedTaskClaimCancelerSchedulerHealthCheck,
      @Qualifier("referencedTaskClaimerSchedulerHealthCheck")
          HealthIndicator referencedTaskClaimerSchedulerHealthCheck,
      @Qualifier("referencedTaskCompleterSchedulerHealthCheck")
          HealthIndicator referencedTaskCompleterSchedulerHealthCheck) {

    schedulerContributors.put("Kadai Task Starter", kadaiTaskStarterSchedulerHealthCheck);
    schedulerContributors.put("Kadai Task Terminator", kadaiTaskTerminatorSchedulerHealthCheck);
    schedulerContributors.put(
        "Referenced Task Claim Canceler", referencedTaskClaimCancelerSchedulerHealthCheck);
    schedulerContributors.put("Referenced Task Claimer", referencedTaskClaimerSchedulerHealthCheck);
    schedulerContributors.put(
        "Referenced Task Completer", referencedTaskCompleterSchedulerHealthCheck);
  }

  @Override
  public HealthContributor getContributor(String name) {
    return schedulerContributors.get(name);
  }

  @Override
  public Iterator<NamedContributor<HealthContributor>> iterator() {
    return schedulerContributors.entrySet().stream()
        .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
        .iterator();
  }
}
