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
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for claiming tasks from Kadai to Camunda 8. Tests the synchronisation of assignees when
 * tasks get claimed in Kadai.
 */
@KadaiAdapterCamunda8SpringBootTest
class Camunda8TaskClaimerTest extends AbsIntegrationTest {

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

    kadaiEngine.getTaskService().claim(kadaiTask.getId());

    final Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");
    assertThat(claimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);
    Thread.sleep(10000);

    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
    String camundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
    assertThat(camundaAssignee).isEqualTo("admin");
  }

  @Test
  @WithAccessId(user = "admin")
  void should_ClaimAlreadyClaimedCamundaTask_When_ClaimKadaiTask() throws Exception {
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

    assignCamundaTask(camundaTaskKey, "user-1-1");
    Thread.sleep(10000);

    String initialCamundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
    assertThat(initialCamundaAssignee).isEqualTo("user-1-1");

    assertThat(kadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.READY);

    kadaiEngine.getTaskService().claim(kadaiTask.getId());

    final Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");

    Thread.sleep(10000);

    String camundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
    assertThat(camundaAssignee).isEqualTo("admin");
  }

  @Test
  @WithAccessId(user = "admin")
  void should_CancelClaimCamundaTask_When_KadaiTaskIsCancelClaimed() throws Exception {
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

    final Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");
    assertThat(claimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);
    Thread.sleep(10000);

    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
    String camundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
    assertThat(camundaAssignee).isEqualTo("admin");

    kadaiEngine.getTaskService().cancelClaim(kadaiTask.getId());
    Task cancelClaimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(cancelClaimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.READY);
    Thread.sleep(10000);
    camundaAssignee = getCamundaTaskAssignee(camundaTaskKey);
    assertThat(camundaAssignee).isEqualTo(null);
  }

  @Test
  @WithAccessId(user = "admin")
  void should_ClaimCamundaTaskAgain_When_ClaimKadaiTaskAfterCancelClaim() throws Exception {
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

    Thread.sleep(10000);

    final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    Thread.sleep(10000);
    assertThat(getCamundaTaskAssignee(camundaTaskKey)).isEqualTo("admin");

    kadaiEngine.getTaskService().cancelClaim(kadaiTask.getId());
    Thread.sleep(10000);
    assertThat(getCamundaTaskAssignee(camundaTaskKey)).isNull();

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    Thread.sleep(10000);

    Task finalKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(finalKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);
    assertThat(getCamundaTaskAssignee(camundaTaskKey)).isEqualTo("admin");
  }
}
