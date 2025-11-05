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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for claiming tasks from Kadai to Camunda 8. Tests the synchronisation of assignees when
 * tasks get claimed in Kadai.
 */
@KadaiAdapterCamunda8SpringBootTest
class Camunda8TaskClaimerTest {

  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired Camunda8TestUtil camunda8TestUtil;

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
    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));
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

    camunda8TestUtil.assignCamundaTask(camundaTaskKey, "user-1-1");
    camunda8TestUtil.waitUntil(
        () -> "user-1-1".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));

    assertThat(kadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.READY);
    kadaiEngine.getTaskService().claim(kadaiTask.getId());

    final Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");

    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));
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

    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));
    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));

    kadaiEngine.getTaskService().cancelClaim(kadaiTask.getId());
    Task cancelClaimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(cancelClaimedKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.READY);
    camunda8TestUtil.waitUntil(
        () -> camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey) == null);
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

    final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(1);

    final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
    String externalId = kadaiTask.getExternalId();
    long camundaTaskKey = Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));

    kadaiEngine.getTaskService().cancelClaim(kadaiTask.getId());
    camunda8TestUtil.waitUntil(
        () -> camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey) == null);

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    Task finalKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(finalKadaiTask.getState()).isEqualTo(io.kadai.task.api.TaskState.CLAIMED);

    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));
  }
}
