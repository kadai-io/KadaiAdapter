package io.kadai.adapter.systemconnector.camunda;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.search.response.UserTask;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.ThrowingConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class for Camunda 8 test operations. Similar to KadaiAdapterTestUtil but for Camunda 8
 * specific operations.
 */
@Component
public class Camunda8TestUtil {

  @Autowired private CamundaClient camundaClient;

  public String getCamundaTaskAssignee(long taskKey) {
    try {
      UserTask task = getCamundaTask(taskKey);
      return task.getAssignee();
    } catch (Exception e) {
      return null;
    }
  }

  public String getCamundaTaskStatus(long taskKey) {
    try {
      UserTask task = getCamundaTask(taskKey);
      return task.getState().name();
    } catch (Exception e) {
      return null;
    }
  }

  public void assignCamundaTask(long taskKey, String assignee) {
    try {
      camundaClient.newAssignUserTaskCommand(taskKey).assignee(assignee).send().join();
    } catch (Exception e) {
      throw new RuntimeException("Failed to assign Camunda task", e);
    }
  }

  public void waitUntil(Callable<Boolean> condition) {
    await()
        .pollInSameThread()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(condition);
  }

  public UserTask getCamundaTask(long taskKey) {
    try {
      return camundaClient.newUserTaskGetRequest(taskKey).send().join();
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Camunda task", e);
    }
  }

  public static boolean handleNextJob(
      CamundaClient workerClient,
      String workerName,
      String tenantId,
      String jobType,
      ThrowingConsumer<ActivatedJob> handler) {
    List<ActivatedJob> jobs =
        workerClient
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(1)
            .workerName(workerName)
            .timeout(Duration.ofSeconds(30))
            .tenantId(tenantId)
            .requestTimeout(Duration.ofSeconds(1))
            .send()
            .join()
            .getJobs();

    if (jobs.isEmpty()) {
      return false;
    }

    ActivatedJob job = jobs.get(0);
    try {
      handler.accept(job);
      workerClient.newCompleteCommand(job).send().join();
      return true;
    } catch (Exception e) {
      throw new RuntimeException("Failed to handle job type '" + jobType + "'", e);
    }
  }
}
