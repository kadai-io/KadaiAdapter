package io.kadai.adapter.systemconnector.camunda;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.UserTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for Camunda 8 test operations. Similar to KadaiAdapterTestUtil but for Camunda 8
 * specific operations.
 */
@Component
public class Camunda8TestUtil {

  private final ObjectMapper mapper = new ObjectMapper();
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
    await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(condition);
  }

  public UserTask getCamundaTask(long taskKey) {
    try {
      return camundaClient.newUserTaskGetRequest(taskKey).send().join();
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Camunda task", e);
    }
  }
}
