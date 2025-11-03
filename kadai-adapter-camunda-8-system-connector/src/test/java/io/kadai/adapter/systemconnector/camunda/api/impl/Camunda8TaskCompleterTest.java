package io.kadai.adapter.systemconnector.camunda.api.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.adapter.systemconnector.camunda.tasklistener.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for completing tasks from Kadai to Camunda 8. Tests the synchronisation of status when
 * tasks get completed in Kadai.
 */
@KadaiAdapterCamunda8SpringBootTest
public class Camunda8TaskCompleterTest extends AbsIntegrationTest {
  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private Camunda8System camunda8System;
  @Autowired private Camunda8HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;

  @BeforeEach
  void setup() {
    camunda8System.setClusterApiUrl(client.getConfiguration().getRestAddress().toString());
  }

  @Test
  @WithAccessId(user = "admin")
  void should_CompleteCamundaTask_When_KadaiTaskIsCompleted() throws Exception {
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

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

    final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
    Thread.sleep(10000);

    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
    String state = getCamundaTaskStatus(camundaTaskKey);
    assertThat(state).isEqualTo("COMPLETED");
  }
}
