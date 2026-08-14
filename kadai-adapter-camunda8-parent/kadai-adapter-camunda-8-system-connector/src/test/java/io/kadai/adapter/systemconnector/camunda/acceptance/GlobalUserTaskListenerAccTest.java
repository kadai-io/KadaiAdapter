package io.kadai.adapter.systemconnector.camunda.acceptance;

import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.CANCELING;
import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.COMPLETING;
import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.CREATING;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation.USER_TASK_CANCELLED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion.USER_TASK_COMPLETED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation.USER_TASK_CREATED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.enums.GlobalTaskListenerEventType;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.TestDeployment;
import io.kadai.adapter.monitoring.MonitoredComponent;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@KadaiAdapterCamunda8SpringBootTest
class GlobalUserTaskListenerAccTest {

  private static final String PROCESS_ID = "Global_User_Task_Listeners_Process";
  private static final String CREATE_LISTENER_ID = "kadai-test-global-create-task";
  private static final String COMPLETE_LISTENER_ID = "kadai-test-global-complete-task";
  private static final String CANCEL_LISTENER_ID = "kadai-test-global-cancel-task";

  @Autowired private CamundaClient client;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private UserTaskCancellation userTaskCancellation;
  @Autowired private UserTaskCompletion userTaskCompletion;
  @Autowired private UserTaskCreation userTaskCreation;

  @BeforeEach
  void createGlobalUserTaskListeners() {
    deleteGlobalUserTaskListeners();

    createGlobalUserTaskListener(CREATE_LISTENER_ID, USER_TASK_CREATED_JOB_WORKER_TYPE, CREATING);
    createGlobalUserTaskListener(
        COMPLETE_LISTENER_ID, USER_TASK_COMPLETED_JOB_WORKER_TYPE, COMPLETING);
    createGlobalUserTaskListener(
        CANCEL_LISTENER_ID, USER_TASK_CANCELLED_JOB_WORKER_TYPE, CANCELING);
  }

  @AfterEach
  void deleteGlobalUserTaskListeners() {
    deleteGlobalUserTaskListener(CREATE_LISTENER_ID);
    deleteGlobalUserTaskListener(COMPLETE_LISTENER_ID);
    deleteGlobalUserTaskListener(CANCEL_LISTENER_ID);
  }

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/globalUserTaskListenersProcess.bpmn")
  void should_CreateKadaiTask_When_CreatingListenerIsConfiguredGlobally() throws Exception {
    createWorkbasketAndClassification();

    Instant globalListenerTriggerTime = Instant.now();
    createKadaiTaskViaGlobalUserTaskCreationListener(randomCorrelationKey());

    assertGlobalUserTaskListenerTriggered(userTaskCreation, globalListenerTriggerTime);
  }

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/globalUserTaskListenersProcess.bpmn")
  void should_CompleteKadaiTask_When_CompletingListenerIsConfiguredGlobally() throws Exception {
    createWorkbasketAndClassification();

    Task taskToComplete = createKadaiTaskViaGlobalUserTaskCreationListener(randomCorrelationKey());
    Instant globalListenerTriggerTime = Instant.now();
    completeKadaiTaskViaGlobalUserTaskCompletionListener(taskToComplete);

    assertGlobalUserTaskListenerTriggered(userTaskCompletion, globalListenerTriggerTime);
  }

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/globalUserTaskListenersProcess.bpmn")
  void should_CancelKadaiTask_When_CancelingListenerIsConfiguredGlobally() throws Exception {
    createWorkbasketAndClassification();

    String cancellationCorrelationKey = randomCorrelationKey();
    Task taskToCancel =
        createKadaiTaskViaGlobalUserTaskCreationListener(cancellationCorrelationKey);
    Instant globalListenerTriggerTime = Instant.now();
    cancelKadaiTaskViaGlobalUserTaskCancellationListener(taskToCancel, cancellationCorrelationKey);

    assertGlobalUserTaskListenerTriggered(userTaskCancellation, globalListenerTriggerTime);
  }

  private void createWorkbasketAndClassification() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
  }

  private void createGlobalUserTaskListener(
      String id, String jobType, GlobalTaskListenerEventType eventType) {
    client
        .newCreateGlobalTaskListenerRequest()
        .id(id)
        .type(jobType)
        .eventTypes(eventType)
        .priority(50)
        .send()
        .join();
  }

  private void deleteGlobalUserTaskListener(String id) {
    try {
      client.newDeleteGlobalTaskListenerRequest(id).send().join();
    } catch (RuntimeException ignored) {
      // Listener cleanup is best-effort. The listener may not exist before the test creates it.
    }
  }

  private Task createKadaiTaskViaGlobalUserTaskCreationListener(String correlationKey) {
    ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(PROCESS_ID)
            .latestVersion()
            .variable("correlationKey", correlationKey)
            .send()
            .join();

    CamundaAssert.assertThat(processInstance).isActive();

    camunda8TestUtil.waitUntil(
        () ->
            getKadaiTasksByProcessInstance(processInstance.getProcessInstanceKey()).stream()
                .anyMatch(task -> task.getState() == TaskState.READY));

    Task task = getOnlyKadaiTaskByProcessInstance(processInstance.getProcessInstanceKey());
    assertThat(task.getState()).isEqualTo(TaskState.READY);
    assertThat(task.getDomain()).isEqualTo("DOMAIN_A");
    assertThat(task.getName()).isEqualTo("Review request");
    return task;
  }

  private void completeKadaiTaskViaGlobalUserTaskCompletionListener(Task kadaiTask) {
    long userTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());

    client.newCompleteUserTaskCommand(userTaskKey).send().join();

    camunda8TestUtil.waitUntil(
        () ->
            kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
                == TaskState.COMPLETED);

    Task completedTask = getKadaiTask(kadaiTask.getId());
    assertThat(completedTask.getState()).isEqualTo(TaskState.COMPLETED);
  }

  private void cancelKadaiTaskViaGlobalUserTaskCancellationListener(
      Task kadaiTask, String correlationKey) {
    client
        .newPublishMessageCommand()
        .messageName("cancel-review")
        .correlationKey(correlationKey)
        .timeToLive(Duration.ofMinutes(1))
        .send()
        .join();

    camunda8TestUtil.waitUntil(
        () ->
            kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
                == TaskState.CANCELLED);

    Task cancelledTask = getKadaiTask(kadaiTask.getId());
    assertThat(cancelledTask.getState()).isEqualTo(TaskState.CANCELLED);
  }

  private String randomCorrelationKey() {
    return UUID.randomUUID().toString();
  }

  private Task getOnlyKadaiTaskByProcessInstance(long processInstanceKey) {
    List<Task> tasks = getKadaiTasksByProcessInstance(processInstanceKey);
    assertThat(tasks).hasSize(1);
    return tasks.getFirst();
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

  private void assertGlobalUserTaskListenerTriggered(
      MonitoredComponent monitoredComponent, Instant globalListenerTriggerTime) {
    camunda8TestUtil.waitUntil(
        () -> {
          var lastRun = monitoredComponent.getLastRun();
          return lastRun.getEnd() != null
              && lastRun.getStart() != null
              && !lastRun.getStart().isBefore(globalListenerTriggerTime);
        });

    assertThat(monitoredComponent.getLastRun().getEnd()).isNotNull();
    assertThat(monitoredComponent.getLastRun().getStart())
        .isAfterOrEqualTo(globalListenerTriggerTime);
    assertThat(monitoredComponent.getLastRun().isSuccessful()).isTrue();
  }
}
