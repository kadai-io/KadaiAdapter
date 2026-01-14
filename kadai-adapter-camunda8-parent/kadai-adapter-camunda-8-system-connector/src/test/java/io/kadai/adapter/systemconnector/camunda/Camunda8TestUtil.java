package io.kadai.adapter.systemconnector.camunda;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.search.response.UserTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
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

  public long extractCamundaTaskKeyFromExternalId(String externalId) {
    return Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
  }

  public long extractCamundaTaskKey(io.kadai.task.api.models.Task kadaiTask) {
    String externalId = kadaiTask.getExternalId();
    return extractCamundaTaskKeyFromExternalId(externalId);
  }

  public void waitUntil(Callable<Boolean> condition) {
    await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(condition);
  }

  private UserTask getCamundaTask(long taskKey) {
    try {
      return camundaClient.newUserTaskGetRequest(taskKey).send().join();
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Camunda task", e);
    }
  }

  private String extractAssigneeFromJson(String jsonResponse) {
    try {
      JsonNode root = mapper.readTree(jsonResponse);
      JsonNode assigneeNode = root.get("assignee");
      return assigneeNode != null && !assigneeNode.isNull() ? assigneeNode.asText() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private String extractStateFromJson(String jsonResponse) {
    try {
      JsonNode root = mapper.readTree(jsonResponse);
      JsonNode stateNode = root.get("state");
      return stateNode != null && !stateNode.isNull() ? stateNode.asText() : null;
    } catch (Exception e) {
      return null;
    }
  }
}
