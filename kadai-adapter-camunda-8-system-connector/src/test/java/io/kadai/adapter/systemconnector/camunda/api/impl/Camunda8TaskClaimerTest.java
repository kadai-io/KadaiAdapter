package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;

import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.adapter.systemconnector.camunda.tasklistener.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import io.kadai.common.test.security.WithAccessId;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests for claiming tasks from Kadai to Camunda 8.
 * Tests the synchronisation of assignees when tasks get claimed in Kadai.
 */
@KadaiAdapterCamunda8SpringBootTest
class Camunda8TaskClaimerTest {

  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private Camunda8System camunda8System;
  @Autowired private Camunda8HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;
  @Autowired private ApplicationContext applicationContext;

  @BeforeEach
  void setup() {
    camunda8System.setClusterApiUrl(client.getConfiguration().getRestAddress().toString());
  }

  @Test
  @WithAccessId(user = "admin")
  void should_ClaimCamundaTask_When_KadaiTaskIsClaimed() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
    client
            .newDeployResourceCommand()
            .addResourceFromClasspath("processes/sayHello.bpmn")
            .send()
            .join();

    final ProcessInstanceEvent processInstance =
            client
                    .newCreateInstanceCommand()
                    .bpmnProcessId("Test_Process")
                    .latestVersion()
                    .send()
                    .join();

    CamundaAssert.assertThat(processInstance).isActive();

    final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

    kadaiEngine.getTaskService().claim(kadaiTask.getId());

    final Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");
    assertThat(claimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);

    await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(250))
        .untilAsserted(() -> {
          String camundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
          assertThat(camundaAssignee).isEqualTo("CLAIMED");
        });
  }


  private String getCamundaTaskAssignee(long taskKey) {
    String requestUrl = client.getConfiguration().getRestAddress() + "/v2/user-tasks/" + taskKey;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

      ResponseEntity<String> response = restTemplate.exchange(
          requestUrl,
          HttpMethod.GET,
          requestEntity,
          String.class
      );

      return extractStateFromJson(response.getBody());
  }

  private String extractStateFromJson(String jsonResponse) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(jsonResponse);
      return root.get("state").asText();
    } catch (Exception e) {
      return null;
    }
  }
  //TODO: check if in Camunda is claimed too (Camunda8ClusterApiRequester)
}
