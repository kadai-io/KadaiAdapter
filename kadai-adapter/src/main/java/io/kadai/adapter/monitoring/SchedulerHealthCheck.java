package io.kadai.adapter.monitoring;

import io.kadai.adapter.monitoring.schedulers.KadaiTaskStarterSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.KadaiTaskTerminatorSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskClaimCancelerSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskClaimerSchedulerHealthCheck;
import io.kadai.adapter.monitoring.schedulers.ReferencedTaskCompleterSchedulerHealthCheck;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.stereotype.Component;

@Component("schedulerHealthCheck")
public class SchedulerHealthCheck implements CompositeHealthContributor {

  private final Map<String, HealthContributor> schedulerContributors = new LinkedHashMap<>();

  @Autowired
  public SchedulerHealthCheck(
      KadaiTaskStarterSchedulerHealthCheck kadaiTaskStarterSchedulerHealthCheck,
      KadaiTaskTerminatorSchedulerHealthCheck kadaiTaskTerminatorSchedulerHealthCheck,
      ReferencedTaskClaimCancelerSchedulerHealthCheck
          referencedTaskClaimCancelerSchedulerHealthCheck,
      ReferencedTaskClaimerSchedulerHealthCheck referencedTaskClaimerSchedulerHealthCheck,
      ReferencedTaskCompleterSchedulerHealthCheck referencedTaskCompleterSchedulerHealthCheck) {

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
