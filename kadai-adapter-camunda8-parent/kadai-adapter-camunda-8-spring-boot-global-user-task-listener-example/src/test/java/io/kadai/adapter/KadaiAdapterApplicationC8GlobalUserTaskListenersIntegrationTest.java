package io.kadai.adapter;

import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.CREATING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.TestDeployment;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8ExampleTest;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@KadaiAdapterCamunda8ExampleTest
class KadaiAdapterApplicationC8GlobalUserTaskListenersIntegrationTest {

  private static final String PROCESS_ID = "Global_User_Task_Listeners_Process";
  private static final String CREATE_LISTENER_ID = "kadai-create-task";

  @Autowired private CamundaClient client;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private KadaiAdapterApplicationC8GlobalUserTaskListeners application;
  @MockitoBean(name = "configureGlobalUserTaskListenersRunner")
  private ApplicationRunner globalUserTaskListenersRunner;

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/globalUserTaskListenersProcess.bpmn")
  void should_ConfigureGlobalListenerAndCreateKadaiTask_When_ExampleApplicationStarts()
      throws Exception {
    application.configureGlobalUserTaskListeners(client);

    assertThat(client.newGlobalTaskListenerGetRequest(CREATE_LISTENER_ID).send().join().getType())
        .isEqualTo(UserTaskCreation.USER_TASK_CREATED_JOB_WORKER_TYPE);
    assertThat(
            client
                .newGlobalTaskListenerGetRequest(CREATE_LISTENER_ID)
                .send()
                .join()
                .getEventTypes())
        .containsExactly(CREATING);

    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

    ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(PROCESS_ID)
            .latestVersion()
            .variable("correlationKey", "example-global-listener-test")
            .send()
            .join();

    CamundaAssert.assertThat(processInstance).isActive();
    camunda8TestUtil.waitUntil(
        () ->
            getKadaiTasks(processInstance.getProcessInstanceKey()).stream()
                .anyMatch(task -> task.getState() == TaskState.READY));

    assertThat(getKadaiTasks(processInstance.getProcessInstanceKey()))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.getState()).isEqualTo(TaskState.READY);
              assertThat(task.getName()).isEqualTo("Review request");
              assertThat(task.getDomain()).isEqualTo("DOMAIN_A");
            });
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
