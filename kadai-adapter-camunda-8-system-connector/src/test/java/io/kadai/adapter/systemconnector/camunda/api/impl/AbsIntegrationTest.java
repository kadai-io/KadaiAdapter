package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.adapter.systemconnector.camunda.tasklistener.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for Camunda 8 integration tests providing common functionality for task operations and
 * Camunda API interactions.
 */
@KadaiAdapterCamunda8SpringBootTest
public abstract class AbsIntegrationTest {

  @Autowired protected CamundaClient client;
  @Autowired protected KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired protected KadaiEngine kadaiEngine;
  @Autowired protected Camunda8System camunda8System;
  @Autowired protected Camunda8HttpHeaderProvider httpHeaderProvider;
  @Autowired protected RestTemplate restTemplate;

  protected final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    camunda8System.setClusterApiUrl(client.getConfiguration().getRestAddress().toString());
  }

  protected String getCamundaTaskAssignee(long taskKey) {
    try {
      String jsonResponse = getCamundaTaskJson(taskKey);
      return extractAssigneeFromJson(jsonResponse);
    } catch (Exception e) {
      return null;
    }
  }

  protected String getCamundaTaskStatus(long taskKey) {
    try {
      String jsonResponse = getCamundaTaskJson(taskKey);
      return extractStateFromJson(jsonResponse);
    } catch (Exception e) {
      return null;
    }
  }

  protected void assignCamundaTask(long taskKey, String assignee) {
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
}
