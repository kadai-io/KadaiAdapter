package io.kadai.adapter.systemconnector.camunda.acceptance;

import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.spring.properties.CamundaClientProperties;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.TestDeployment;
import io.kadai.adapter.monitoring.MonitoredComponent;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractJobWorkerCommunicationAccTest {

  private static final String PROCESS_ID = "Test_Process";

  @Autowired private CamundaClient client;
  @Autowired private CamundaClientProperties clientProperties;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private UserTaskCancellation userTaskCancellation;
  @Autowired private UserTaskCompletion userTaskCompletion;
  @Autowired private UserTaskCreation userTaskCreation;

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/sayHello.bpmn")
  void should_RunAllProductionJobWorkersWithConfiguredCommunication() throws Exception {
    assertThat(clientProperties.getPreferRestOverGrpc()).isEqualTo(expectedPreferRestOverGrpc());
    assertThat(clientProperties.getWorker().getDefaults().getStreamEnabled())
        .isEqualTo(expectedStreamEnabled());

    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

    Task taskToComplete = createKadaiTaskViaUserTaskCreationJobWorker();
    completeKadaiTaskViaUserTaskCompletionJobWorker(taskToComplete);

    Task taskToCancel = createKadaiTaskViaUserTaskCreationJobWorker();
    cancelKadaiTaskViaUserTaskCancellationJobWorker(taskToCancel);

    assertSuccessfulRun(userTaskCreation);
    assertSuccessfulRun(userTaskCompletion);
    assertSuccessfulRun(userTaskCancellation);
  }

  protected abstract boolean expectedPreferRestOverGrpc();

  protected boolean expectedStreamEnabled() {
    return false;
  }

  private Task createKadaiTaskViaUserTaskCreationJobWorker() {
    var processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(PROCESS_ID)
            .latestVersion()
            .send()
            .join();

    CamundaAssert.assertThat(processInstance).isActive();

    camunda8TestUtil.waitUntil(
        () ->
            getKadaiTasksByProcessInstance(processInstance.getProcessInstanceKey()).stream()
                .anyMatch(task -> task.getState() == TaskState.READY));

    Task task = getOnlyKadaiTaskByProcessInstance(processInstance.getProcessInstanceKey());
    assertThat(task.getState()).isEqualTo(TaskState.READY);
    return task;
  }

  private void completeKadaiTaskViaUserTaskCompletionJobWorker(Task kadaiTask) {
    long userTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());

    client.newCompleteUserTaskCommand(userTaskKey).send().join();

    camunda8TestUtil.waitUntil(
        () -> kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
            == TaskState.COMPLETED);

    Task completedTask = getKadaiTask(kadaiTask.getId());
    assertThat(completedTask.getState()).isEqualTo(TaskState.COMPLETED);
  }

  private void cancelKadaiTaskViaUserTaskCancellationJobWorker(Task kadaiTask) {
    client
        .newCancelInstanceCommand(Long.parseLong(kadaiTask.getBusinessProcessId()))
        .send()
        .join();

    camunda8TestUtil.waitUntil(
        () -> kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
            == TaskState.CANCELLED);

    Task cancelledTask = getKadaiTask(kadaiTask.getId());
    assertThat(cancelledTask.getState()).isEqualTo(TaskState.CANCELLED);
  }

  private Task getOnlyKadaiTaskByProcessInstance(long processInstanceKey) {
    List<Task> tasks = getKadaiTasksByProcessInstance(processInstanceKey);
    assertThat(tasks).hasSize(1);
    return tasks.get(0);
  }

  private List<Task> getKadaiTasksByProcessInstance(long processInstanceKey) {
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

  private void assertSuccessfulRun(MonitoredComponent monitoredComponent) {
    assertThat(monitoredComponent.getLastRun().getEnd()).isNotNull();
    assertThat(monitoredComponent.getLastRun().isSuccessful()).isTrue();
    assertThat(monitoredComponent.getExpectedRunDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
  }
}
