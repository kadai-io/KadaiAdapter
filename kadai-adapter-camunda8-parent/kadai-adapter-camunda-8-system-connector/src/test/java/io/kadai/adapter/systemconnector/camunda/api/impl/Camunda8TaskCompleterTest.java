package io.kadai.adapter.systemconnector.camunda.api.impl;

import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation.USER_TASK_CANCELLED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion.USER_TASK_COMPLETED_JOB_WORKER_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.process.test.api.CamundaAssert;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
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

/**
 * Tests for completing tasks from Kadai to Camunda 8. Tests the synchronisation of status when
 * tasks get completed in Kadai.
 */
@DirtiesContext
class Camunda8TaskCompleterTest {

  @Nested
  @KadaiAdapterCamunda8SpringBootTest
  class NoMultiTenancyCamunda8TaskCompleterTest {
    @Autowired Camunda8TestUtil camunda8TestUtil;
    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CompleteCamundaTask_When_KadaiTaskIsCompleted() throws Exception {
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
      kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

      final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
      String externalId = kadaiTask.getExternalId();

      long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

      CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @Test
    @WithAccessId(user = "admin")
    void should_SetCompletedByKadaiAction_When_KadaiTaskIsCompleted() throws Exception {
      final JobHandler verificationHandler =
          (jobClient, job) -> {
            final String action = job.getUserTask().getAction();
            assertThat(action).isEqualTo("completed-by-kadai");
            jobClient.newCompleteCommand(job).send().join();
          };

      try (final JobWorker worker =
          client
              .newWorker()
              .jobType(USER_TASK_COMPLETED_JOB_WORKER_TYPE)
              .handler(verificationHandler)
              .open()) {
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
        final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
        kadaiEngine.getTaskService().claim(kadaiTask.getId());
        kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

        final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
        assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
        String externalId = kadaiTask.getExternalId();

        long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
        camunda8TestUtil.waitUntil(
            () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

        CamundaAssert.assertThat(processInstance).isCompleted();
      }
    }

    @Test
    @WithAccessId(user = "admin")
    void should_CancelCamundaTask_When_KadaiTaskIsCancelled() throws Exception {
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

      kadaiEngine.getTaskService().cancelTask(kadaiTask.getId());

      final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.CANCELLED);
      String externalId = kadaiTask.getExternalId();

      long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

      CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @Test
    @WithAccessId(user = "admin")
    void should_SetCompletedByKadaiAction_When_KadaiTaskIsCancelled() throws Exception {
      final JobHandler verificationHandler =
          (jobClient, job) -> {
            final String action = job.getUserTask().getAction();
            assertThat(action).isEqualTo("completed-by-kadai");
            jobClient.newCompleteCommand(job).send().join();
          };

      try (final JobWorker worker =
          client
              .newWorker()
              .jobType(USER_TASK_CANCELLED_JOB_WORKER_TYPE)
              .handler(verificationHandler)
              .open()) {
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

        kadaiEngine.getTaskService().cancelTask(kadaiTask.getId());

        final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
        assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.CANCELLED);
        String externalId = kadaiTask.getExternalId();

        long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
        camunda8TestUtil.waitUntil(
            () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

        CamundaAssert.assertThat(processInstance).isCompleted();
      }
    }
  }

  @Nested
  @TestPropertySource("classpath:camunda8-mt-test-application.properties")
  @KadaiAdapterCamunda8SpringBootTest
  class MultiTenancyCamunda8TaskCompleterTest {
    @Autowired Camunda8TestUtil camunda8TestUtil;
    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CompleteCamundaTask_When_KadaiTaskIsCompleted() throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

      // create new Tenant and add user to it (user needs access to all tenants)
      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).send().join();
      client
          .newAssignUserToTenantCommand()
          .username(allTenantsUser)
          .tenantId(tenant1)
          .send()
          .join();

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
      kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

      final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
      String externalId = kadaiTask.getExternalId();

      long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

      CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @Test
    @WithAccessId(user = "admin")
    void should_SetCompletedByKadaiAction_When_KadaiTaskIsCompleted() throws Exception {

      // create new Tenant and add user to it (user needs access to all tenants)
      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).send().join();
      client
          .newAssignUserToTenantCommand()
          .username(allTenantsUser)
          .tenantId(tenant1)
          .send()
          .join();

      final JobHandler verificationHandler =
          (jobClient, job) -> {
            final String action = job.getUserTask().getAction();
            assertThat(action).isEqualTo("completed-by-kadai");
            jobClient.newCompleteCommand(job).send().join();
          };

      try (final JobWorker worker =
          client
              .newWorker()
              .jobType(USER_TASK_COMPLETED_JOB_WORKER_TYPE)
              .handler(verificationHandler)
              .open()) {
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
        final Task kadaiTask = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
        kadaiEngine.getTaskService().claim(kadaiTask.getId());
        kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

        final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
        assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
        String externalId = kadaiTask.getExternalId();

        long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
        camunda8TestUtil.waitUntil(
            () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

        CamundaAssert.assertThat(processInstance).isCompleted();
      }
    }

    @Test
    @WithAccessId(user = "admin")
    void should_CancelCamundaTask_When_KadaiTaskIsCancelled() throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

      // create new Tenant and add user to it (user needs access to all tenants)
      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).send().join();
      client
          .newAssignUserToTenantCommand()
          .username(allTenantsUser)
          .tenantId(tenant1)
          .send()
          .join();

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

      kadaiEngine.getTaskService().cancelTask(kadaiTask.getId());

      final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.CANCELLED);
      String externalId = kadaiTask.getExternalId();

      long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

      CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @Test
    @WithAccessId(user = "admin")
    void should_SetCompletedByKadaiAction_When_KadaiTaskIsCancelled() throws Exception {

      // create new Tenant and add user to it (user needs access to all tenants)
      final String tenant1 = "tenant1";
      final String allTenantsUser = "demo";
      client.newCreateTenantCommand().tenantId(tenant1).name(tenant1).send().join();
      client
          .newAssignUserToTenantCommand()
          .username(allTenantsUser)
          .tenantId(tenant1)
          .send()
          .join();

      final JobHandler verificationHandler =
          (jobClient, job) -> {
            final String action = job.getUserTask().getAction();
            assertThat(action).isEqualTo("completed-by-kadai");
            jobClient.newCompleteCommand(job).send().join();
          };

      try (final JobWorker worker =
          client
              .newWorker()
              .jobType(USER_TASK_CANCELLED_JOB_WORKER_TYPE)
              .handler(verificationHandler)
              .open()) {
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

        kadaiEngine.getTaskService().cancelTask(kadaiTask.getId());

        final Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
        assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.CANCELLED);
        String externalId = kadaiTask.getExternalId();

        long camundaTaskKey = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(externalId);
        camunda8TestUtil.waitUntil(
            () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));

        CamundaAssert.assertThat(processInstance).isCompleted();
      }
    }
  }
}
