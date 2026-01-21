package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
class UserTaskCompletionTest {

  @Nested
  @KadaiAdapterCamunda8SpringBootTest
  class NoMultiTenancyUserTaskCompletionTest {
    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CompleteKadaiTask_When_CamundaTaskIsCompleted() throws Exception {
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
      final String externalId = kadaiTask.getExternalId();
      final long userTaskKey =
          Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

      client.newCompleteUserTaskCommand(userTaskKey).send().join();

      final Task completedTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedTask.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    @WithAccessId(user = "admin")
    void should_BeIdempotent_When_TaskCompletedTwice() throws Exception {
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
      final String externalId = kadaiTask.getExternalId();
      final long userTaskKey =
          Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

      client.newCompleteUserTaskCommand(userTaskKey).send().join();

      Task afterFirstCompletion = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(afterFirstCompletion.getState()).isEqualTo(TaskState.COMPLETED);

      assertThatThrownBy(() -> completeUserTask(userTaskKey)).isInstanceOf(ProblemException.class);

      Task afterSecondAttempt = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(afterSecondAttempt.getState()).isEqualTo(TaskState.COMPLETED);
    }

    private void completeUserTask(long userTaskKey) {
      client.newCompleteUserTaskCommand(userTaskKey).send().join();
    }
  }

  @Nested
  @TestPropertySource("classpath:camunda8-mt-test-application.properties")
  @KadaiAdapterCamunda8SpringBootTest
  class MultiTenancyUserTaskCompletionTest {

    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CompleteKadaiTask_When_CamundaTaskIsCompleted() throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).execute();
      client.newAssignUserToTenantCommand().username(allTenantsUser).tenantId(tenant1).execute();

      client
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .tenantId(tenant1)
          .send()
          .join();

      final ProcessInstanceEvent processInstance =
          client
              .newCreateInstanceCommand()
              .bpmnProcessId("Test_Process")
              .latestVersion()
              .tenantId(tenant1)
              .send()
              .join();

      CamundaAssert.assertThat(processInstance).isActive();

      final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
      assertThat(tasks).hasSize(1);
      assertThat(tasks.get(0).getState()).isEqualTo(TaskState.READY);

      final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
      final String externalId = kadaiTask.getExternalId();
      final long userTaskKey =
          Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

      client.newCompleteUserTaskCommand(userTaskKey).send().join();
      assertThat(client.newUserTaskGetRequest(userTaskKey).send().join().getTenantId())
          .isEqualTo(tenant1);
      final Task completedTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedTask.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    @WithAccessId(user = "admin")
    void should_BeIdempotent_When_TaskCompletedTwice() throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

      final String defaultTenant = "<default>";
      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).execute();
      client.newAssignUserToTenantCommand().username(allTenantsUser).tenantId(tenant1).execute();

      client
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .tenantId(defaultTenant)
          .send()
          .join();

      final ProcessInstanceEvent processInstance =
          client
              .newCreateInstanceCommand()
              .bpmnProcessId("Test_Process")
              .latestVersion()
              .tenantId(defaultTenant)
              .send()
              .join();

      CamundaAssert.assertThat(processInstance).isActive();

      final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
      assertThat(tasks).hasSize(1);
      assertThat(tasks.get(0).getState()).isEqualTo(TaskState.READY);

      final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
      final String externalId = kadaiTask.getExternalId();
      final long userTaskKey =
          Long.parseLong(externalId.substring(externalId.lastIndexOf("-") + 1));

      client.newCompleteUserTaskCommand(userTaskKey).send().join();

      Task afterFirstCompletion = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(afterFirstCompletion.getState()).isEqualTo(TaskState.COMPLETED);

      assertThatThrownBy(() -> completeUserTask(userTaskKey)).isInstanceOf(ProblemException.class);

      Task afterSecondAttempt = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(afterSecondAttempt.getState()).isEqualTo(TaskState.COMPLETED);
    }

    private void completeUserTask(long userTaskKey) {
      client.newCompleteUserTaskCommand(userTaskKey).send().join();
    }
  }
}
