package io.kadai.adapter.systemconnector.camunda.api.impl;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.camunda.client.CamundaClient;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class for Camunda 8 test operations. Similar to KadaiAdapterTestUtil but for Camunda 8
 * specific operations.
 */
@Component
public class Camunda8TestUtil {

  @Autowired private CamundaClient client;

  @Autowired private Camunda8HttpHeaderProvider httpHeaderProvider;

  @Autowired private RestTemplate restTemplate;

  private final ObjectMapper mapper = new ObjectMapper();

  public String getCamundaTaskAssignee(long taskKey) {
    try {
      String jsonResponse = getCamundaTaskJson(taskKey);
      return extractAssigneeFromJson(jsonResponse);
    } catch (Exception e) {
      return null;
    }
  }

  public String getCamundaTaskStatus(long taskKey) {
    try {
      String jsonResponse = getCamundaTaskJson(taskKey);
      return extractStateFromJson(jsonResponse);
    } catch (Exception e) {
      return null;
    }
  }

  public void assignCamundaTask(long taskKey, String assignee) {
    try {
      String requestUrl =
          client.getConfiguration().getRestAddress() + "/v2/user-tasks/" + taskKey + "/assignment";

      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();

      ObjectNode assignmentRequest = mapper.createObjectNode();
      assignmentRequest.put("assignee", assignee);

      String requestBody = mapper.writeValueAsString(assignmentRequest);
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      ResponseEntity<String> response =
          restTemplate.exchange(requestUrl, HttpMethod.POST, requestEntity, String.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Failed to assign task: " + response.getStatusCode());
      }
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

  private String getCamundaTaskJson(long taskKey) {
    String requestUrl = client.getConfiguration().getRestAddress() + "/v2/user-tasks/" + taskKey;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);

    return response.getBody();
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

  public void waitUntil(Callable<Boolean> condition) {
    await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(condition);
  }
}
