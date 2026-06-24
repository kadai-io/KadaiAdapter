package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.kadai.adapter.impl.util.UserContext;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@KadaiAdapterCamunda8SpringBootTest
class UserTaskCancellationTest {

  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;

  @Test
  @WithAccessId(user = "admin")
  void should_CancelKadaiTask_When_CamundaTaskIsCompleted() throws Exception {
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
    assertThat(tasks.get(0).getState()).isEqualTo(TaskState.READY);

    final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());

    client.newCancelInstanceCommand(processInstance.getProcessInstanceKey()).send().join();

    camunda8TestUtil.waitUntil(
        () ->
            UserContext.runAsUser(
                "admin",
                () ->
                    kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
                        == TaskState.CANCELLED));

    final Task canceledTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(canceledTask.getState()).isEqualTo(TaskState.CANCELLED);
  }
}
