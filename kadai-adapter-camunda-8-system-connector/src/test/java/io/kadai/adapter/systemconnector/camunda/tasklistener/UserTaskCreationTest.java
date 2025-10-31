package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;

import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@KadaiAdapterCamunda8SpringBootTest
class UserTaskCreationTest {

  @Autowired
  private CamundaClient client;
  @Autowired
  private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired
  private KadaiEngine kadaiEngine;

  @Test
  @WithAccessId(user = "admin")
  void should_CreateKadaiTask() throws Exception {
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
    final List<TaskSummary> actual = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(actual).hasSize(1);
    TaskSummary kadaiTask = actual.get(0);
    assertThat(kadaiTask.getState()).isEqualTo(TaskState.READY);
    assertThat(kadaiTask.getWorkbasketSummary().getKey()).isEqualTo("GPK_KSC");
    assertThat(kadaiTask.getClassificationSummary().getKey()).isEqualTo("L11010");
    assertThat(kadaiTask.getDomain()).isEqualTo("DOMAIN_A");
    assertThat(kadaiTask.getName()).isEqualTo("Say Hello Task");
  }

  @Test
  @WithAccessId(user = "admin")
  void should_NotCreateKadaiTask_When_WorkbasketNotFound() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_UNKNOWN", "DOMAIN_A");
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
    final List<TaskSummary> actual = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(actual).hasSize(0);
  }
}
