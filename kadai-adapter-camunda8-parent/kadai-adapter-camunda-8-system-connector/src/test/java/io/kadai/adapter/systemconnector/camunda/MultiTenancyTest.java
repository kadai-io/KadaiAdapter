package io.kadai.adapter.systemconnector.camunda;

import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion.USER_TASK_COMPLETED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation.USER_TASK_CREATED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
@TestPropertySource(
    properties = {
      "camunda.process-test.multi-tenancy-enabled=true",
      "camunda.client.worker.defaults.enabled=false"
    })
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@KadaiAdapterCamunda8SpringBootTest
class MultiTenancyTest {

  private static final String DEFAULT_TENANT = "<default>";
  private static final String TENANT_1 = "tenant1";
  private static final String ALL_TENANTS_USER = "demo";

  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private CamundaProcessTestContext processTestContext;
  @Autowired private UserTaskCreation userTaskCreation;
  @Autowired private UserTaskCompletion userTaskCompletion;

  @Test
  @WithAccessId(user = "admin")
  void should_SynchronizeTenantSpecificTasksEndToEndWithRealWorkers() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
    registerTenant(TENANT_1);

    try (CamundaClient defaultTenantClient =
            processTestContext.createClient(
                clientBuilder -> clientBuilder.defaultTenantId(DEFAULT_TENANT));
        CamundaClient tenantClient =
            processTestContext.createClient(
                clientBuilder -> clientBuilder.defaultTenantId(TENANT_1))) {
      deployProcess(defaultTenantClient, DEFAULT_TENANT);
      deployProcess(tenantClient, TENANT_1);

      final ProcessInstanceEvent defaultProcessInstance =
          startProcessInstance(defaultTenantClient, DEFAULT_TENANT);
      final ProcessInstanceEvent tenantProcessInstance =
          startProcessInstance(tenantClient, TENANT_1);

      CamundaAssert.assertThat(defaultProcessInstance).isActive();
      CamundaAssert.assertThat(tenantProcessInstance).isActive();
      assertThat(tenantProcessInstance.getTenantId()).isEqualTo(TENANT_1);

      camunda8TestUtil.waitUntil(
          () ->
              handleNextJob(
                  defaultTenantClient,
                  "default-create",
                  DEFAULT_TENANT,
                  USER_TASK_CREATED_JOB_WORKER_TYPE,
                  userTaskCreation::receiveTaskCreatedEvent));
      camunda8TestUtil.waitUntil(
          () ->
              handleNextJob(
                  tenantClient,
                  "tenant1-create",
                  TENANT_1,
                  USER_TASK_CREATED_JOB_WORKER_TYPE,
                  userTaskCreation::receiveTaskCreatedEvent));

      camunda8TestUtil.waitUntil(
          () -> kadaiEngine.getTaskService().createTaskQuery().list().size() == 2);

      final Task defaultTenantTask = getKadaiTaskByProcessInstanceKey(defaultProcessInstance);
      final Task tenant1Task = getKadaiTaskByProcessInstanceKey(tenantProcessInstance);

      assertThat(defaultTenantTask.getState()).isEqualTo(TaskState.READY);
      assertThat(tenant1Task.getState()).isEqualTo(TaskState.READY);

      assertThat(getCamundaUserTask(defaultTenantClient, defaultTenantTask).getTenantId())
          .isEqualTo(DEFAULT_TENANT);
      assertThat(getCamundaUserTask(tenantClient, tenant1Task).getTenantId()).isEqualTo(TENANT_1);

      completeKadaiTask(defaultTenantTask);
      camunda8TestUtil.waitUntil(
          () ->
              handleNextJob(
                  defaultTenantClient,
                  "default-complete",
                  DEFAULT_TENANT,
                  USER_TASK_COMPLETED_JOB_WORKER_TYPE,
                  userTaskCompletion::receiveTaskCompletedEvent));

      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(getCamundaTaskStatus(defaultTenantClient, defaultTenantTask)));
      camunda8TestUtil.waitUntil(() -> isProcessCompleted(defaultProcessInstance));

      assertThat(kadaiEngine.getTaskService().getTask(defaultTenantTask.getId()).getState())
          .isEqualTo(TaskState.COMPLETED);
      assertThat(kadaiEngine.getTaskService().getTask(tenant1Task.getId()).getState())
          .isEqualTo(TaskState.READY);
      CamundaAssert.assertThat(defaultProcessInstance).isCompleted();
      CamundaAssert.assertThat(tenantProcessInstance).isActive();

      completeKadaiTask(tenant1Task);
      camunda8TestUtil.waitUntil(
          () ->
              handleNextJob(
                  tenantClient,
                  "tenant1-complete",
                  TENANT_1,
                  USER_TASK_COMPLETED_JOB_WORKER_TYPE,
                  userTaskCompletion::receiveTaskCompletedEvent));

      camunda8TestUtil.waitUntil(
          () -> "COMPLETED".equals(getCamundaTaskStatus(tenantClient, tenant1Task)));
      camunda8TestUtil.waitUntil(() -> isProcessCompleted(tenantProcessInstance));

      assertThat(kadaiEngine.getTaskService().getTask(tenant1Task.getId()).getState())
          .isEqualTo(TaskState.COMPLETED);
      CamundaAssert.assertThat(tenantProcessInstance).isCompleted();
    }
  }

  private boolean handleNextJob(
      CamundaClient workerClient,
      String workerName,
      String tenantId,
      String jobType,
      ThrowingActivatedJobConsumer handler) {
    final List<ActivatedJob> jobs =
        workerClient
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(1)
            .workerName(workerName)
            .timeout(Duration.ofSeconds(30))
            .tenantId(tenantId)
            .requestTimeout(Duration.ofSeconds(1))
            .send()
            .join()
            .getJobs();

    if (jobs.isEmpty()) {
      return false;
    }

    final ActivatedJob job = jobs.get(0);
    try {
      handler.accept(job);
      workerClient.newCompleteCommand(job).send().join();
      return true;
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to handle job type '" + jobType + "' for tenant '" + tenantId + "'", e);
    }
  }

  private Task getKadaiTask(String taskId) {
    try {
      return kadaiEngine.getTaskService().getTask(taskId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load Kadai task '" + taskId + "'", e);
    }
  }

  private Task getKadaiTaskByProcessInstanceKey(ProcessInstanceEvent processInstance) {
    final String processInstanceKey = String.valueOf(processInstance.getProcessInstanceKey());
    return kadaiEngine.getTaskService().createTaskQuery().list().stream()
        .map(taskSummary -> getKadaiTask(taskSummary.getId()))
        .filter(task -> processInstanceKey.equals(String.valueOf(task.getBusinessProcessId())))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "Failed to find KADAI task for process instance '" + processInstanceKey + "'"));
  }

  private UserTask getCamundaUserTask(CamundaClient tenantClient, Task kadaiTask) {
    final long userTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());
    return tenantClient.newUserTaskGetRequest(userTaskKey).send().join();
  }

  private String getCamundaTaskStatus(CamundaClient tenantClient, Task kadaiTask) {
    return getCamundaUserTask(tenantClient, kadaiTask).getState().name();
  }

  private void completeKadaiTask(Task kadaiTask) throws Exception {
    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    kadaiEngine.getTaskService().completeTask(kadaiTask.getId());
  }

  private boolean isProcessCompleted(ProcessInstanceEvent processInstance) {
    try {
      CamundaAssert.assertThat(processInstance).isCompleted();
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

  private void registerTenant(String tenantId) {
    client.newCreateTenantCommand().tenantId(tenantId).name(tenantId).send().join();
    client
        .newAssignUserToTenantCommand()
        .username(ALL_TENANTS_USER)
        .tenantId(tenantId)
        .send()
        .join();
  }

  private void deployProcess(CamundaClient tenantClient, String tenantId) {
    tenantClient
        .newDeployResourceCommand()
        .addResourceFromClasspath("processes/sayHello.bpmn")
        .tenantId(tenantId)
        .send()
        .join();
  }

  private ProcessInstanceEvent startProcessInstance(CamundaClient tenantClient, String tenantId) {
    return tenantClient
        .newCreateInstanceCommand()
        .bpmnProcessId("Test_Process")
        .latestVersion()
        .tenantId(tenantId)
        .send()
        .join();
  }

  @FunctionalInterface
  private interface ThrowingActivatedJobConsumer {
    void accept(ActivatedJob job) throws Exception;
  }
}
