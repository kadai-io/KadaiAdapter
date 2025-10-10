package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.kadai.adapter.systemconnector.camunda.tasklistener.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import io.kadai.common.test.security.WithAccessId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests for claiming tasks from Kadai to Camunda 8.
 * Tests the synchronisation of assignees when tasks get claimed in Kadai.
 */
@KadaiAdapterCamunda8SpringBootTest
class Camunda8TaskClaimerTest {

  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;

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
  }

  //TODO: check if in Camunda is claimed too (Camunda8ClusterApiRequester)
}
