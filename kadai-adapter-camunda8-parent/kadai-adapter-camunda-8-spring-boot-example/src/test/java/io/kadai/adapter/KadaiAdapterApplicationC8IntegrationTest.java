package io.kadai.adapter;

import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.TestDeployment;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8ExampleTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@KadaiAdapterCamunda8ExampleTest
class KadaiAdapterApplicationC8IntegrationTest {

  private static final String PROCESS_ID = "Test_Process";

  @Autowired private CamundaClient client;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/sayHello.bpmn")
  void should_ProcessUserTask_When_ExampleApplicationIsRunning() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

    ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(PROCESS_ID)
            .latestVersion()
            .send()
            .join();

    CamundaAssert.assertThat(processInstance).isActive();
    camunda8TestUtil.waitUntil(
        () ->
            getKadaiTasks(processInstance.getProcessInstanceKey()).stream()
                .anyMatch(task -> task.getState() == TaskState.READY));

    Task kadaiTask = getOnlyKadaiTask(processInstance.getProcessInstanceKey());
    assertThat(kadaiTask.getState()).isEqualTo(TaskState.READY);

    client
        .newCompleteUserTaskCommand(
            extractUserTaskKeyFromTaskId(kadaiTask.getExternalId()))
        .send()
        .join();

    camunda8TestUtil.waitUntil(
        () ->
            kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
                == TaskState.COMPLETED);

    assertThat(kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState())
        .isEqualTo(TaskState.COMPLETED);
    CamundaAssert.assertThat(processInstance).isCompleted();
  }

  private Task getOnlyKadaiTask(long processInstanceKey) {
    List<Task> tasks = getKadaiTasks(processInstanceKey);
    assertThat(tasks).hasSize(1);
    return tasks.getFirst();
  }

  private List<Task> getKadaiTasks(long processInstanceKey) {
    return kadaiEngine.getTaskService().createTaskQuery().list().stream()
        .map(TaskSummary::getId)
        .map(this::getKadaiTask)
        .filter(task -> String.valueOf(processInstanceKey).equals(task.getBusinessProcessId()))
        .toList();
  }

  private Task getKadaiTask(String taskId) {
    try {
      return kadaiEngine.getTaskService().getTask(taskId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load KADAI task '" + taskId + "'", e);
    }
  }
}
