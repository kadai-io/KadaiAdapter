package io.kadai.adapter.systemconnector.camunda.acceptance;

import static io.camunda.client.api.search.enums.PermissionType.CLAIM_USER_TASK;
import static io.camunda.client.api.search.enums.PermissionType.COMPLETE_USER_TASK;
import static io.camunda.client.api.search.enums.PermissionType.READ_USER_TASK;
import static io.camunda.client.api.search.enums.PermissionType.UPDATE_PROCESS_INSTANCE;
import static io.camunda.client.api.search.enums.PermissionType.UPDATE_USER_TASK;
import static io.camunda.client.api.search.enums.ResourceType.PROCESS_DEFINITION;
import static io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil.handleNextJob;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation.USER_TASK_CANCELLED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion.USER_TASK_COMPLETED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation.USER_TASK_CREATED_JOB_WORKER_TYPE;
import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.enums.OwnerType;
import io.camunda.client.api.search.enums.PermissionType;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.TestDeployment;
import io.camunda.process.test.impl.proxy.CamundaClientProxy;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Acceptance test for the minimal Camunda 8 Orchestration Cluster permissions required by the
 * adapter.
 *
 * <p>Camunda Process Test 8.9.12 does not yet provide first-class OAuth2/authorization fixtures.
 * Its managed secure runtime uses HTTP Basic authentication, so this test verifies the permission
 * boundary with a restricted basic-auth user and seeded authorizations.
 */
@DirtiesContext
@TestPropertySource(
    properties = {
      "camunda.process-test.multi-tenancy-enabled=true",
      "camunda.client.worker.defaults.enabled=false"
    })
@KadaiAdapterCamunda8SpringBootTest
class MinimalPermissionsAccTest {

  private static final String DEFAULT_TENANT = "<default>";
  private static final String PROCESS_ID = "Test_Process";
  private static final String RESTRICTED_USERNAME = "kadai-adapter";
  private static final String RESTRICTED_PASSWORD = "kadai-adapter";

  @Autowired private CamundaClientProxy camundaClientProxy;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private CamundaProcessTestContext processTestContext;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private UserTaskCancellation userTaskCancellation;
  @Autowired private UserTaskCompletion userTaskCompletion;
  @Autowired private UserTaskCreation userTaskCreation;

  @Test
  @WithAccessId(user = "admin")
  @TestDeployment(resources = "processes/sayHello.bpmn")
  void should_RunAllAdapterFlowsSuccessfully_WhenMinimalCamunda8PermissionsAreConfigured()
      throws Exception {
    CamundaClient adminClient =
        processTestContext.createClient(builder -> builder.defaultTenantId(DEFAULT_TENANT));

    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
    createRestrictedCamundaUser(adminClient);
    grantMinimalAdapterPermissions(adminClient);

    CamundaClient restrictedClient = createRestrictedClient();
    camundaClientProxy.setDelegate(restrictedClient);
    try {
      Task claimAndCompleteTask =
          createKadaiTaskViaRestrictedCreatedListener(adminClient, restrictedClient);

      claimCamundaTaskViaRestrictedOutboundConnector(claimAndCompleteTask);
      cancelCamundaTaskClaimViaRestrictedOutboundConnector(claimAndCompleteTask);
      completeCamundaTaskViaRestrictedOutboundConnector(restrictedClient, claimAndCompleteTask);

      Task cancelledTask =
          createKadaiTaskViaRestrictedCreatedListener(adminClient, restrictedClient);
      cancelProcessAndKadaiTaskViaRestrictedCancelledListener(
          adminClient, restrictedClient, cancelledTask);
    } finally {
      camundaClientProxy.setDelegate(adminClient);
    }
  }

  private void createRestrictedCamundaUser(CamundaClient adminClient) {
    adminClient
        .newCreateUserCommand()
        .username(RESTRICTED_USERNAME)
        .password(RESTRICTED_PASSWORD)
        .name("KADAI Adapter")
        .email("kadai-adapter@example.org")
        .send()
        .join();
    adminClient
        .newAssignUserToTenantCommand()
        .username(RESTRICTED_USERNAME)
        .tenantId(DEFAULT_TENANT)
        .send()
        .join();
  }

  private void grantMinimalAdapterPermissions(CamundaClient adminClient) {
    createAuthorization(adminClient, UPDATE_PROCESS_INSTANCE);
    createAuthorization(
        adminClient, READ_USER_TASK, CLAIM_USER_TASK, UPDATE_USER_TASK, COMPLETE_USER_TASK);
  }

  private void createAuthorization(CamundaClient adminClient, PermissionType... permissionTypes) {
    adminClient
        .newCreateAuthorizationCommand()
        .ownerId(RESTRICTED_USERNAME)
        .ownerType(OwnerType.USER)
        .resourceId(PROCESS_ID)
        .resourceType(PROCESS_DEFINITION)
        .permissionTypes(permissionTypes)
        .send()
        .join();
  }

  private CamundaClient createRestrictedClient() {
    return processTestContext.createClient(
        builder ->
            builder
                .credentialsProvider(
                    CredentialsProvider.newBasicAuthCredentialsProviderBuilder()
                        .username(RESTRICTED_USERNAME)
                        .password(RESTRICTED_PASSWORD)
                        .build())
                .defaultTenantId(DEFAULT_TENANT));
  }

  private Task createKadaiTaskViaRestrictedCreatedListener(
      CamundaClient adminClient, CamundaClient restrictedClient) {
    ProcessInstanceEvent processInstance = startProcessInstance(adminClient);
    CamundaAssert.assertThat(processInstance).isActive();

    camunda8TestUtil.waitUntil(
        () ->
            handleNextJob(
                restrictedClient,
                "kadai-adapter-create",
                DEFAULT_TENANT,
                USER_TASK_CREATED_JOB_WORKER_TYPE,
                userTaskCreation::receiveTaskCreatedEvent));

    camunda8TestUtil.waitUntil(
        () ->
            getKadaiTasksByProcessInstance(processInstance).stream()
                .anyMatch(task -> task.getState() == TaskState.READY));

    Task task = getOnlyKadaiTaskByProcessInstance(processInstance);
    assertThat(task.getState()).isEqualTo(TaskState.READY);
    return task;
  }

  private ProcessInstanceEvent startProcessInstance(CamundaClient adminClient) {
    return adminClient
        .newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .tenantId(DEFAULT_TENANT)
        .send()
        .join();
  }

  private void claimCamundaTaskViaRestrictedOutboundConnector(Task kadaiTask) throws Exception {
    final long camundaTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());

    kadaiEngine.getTaskService().claim(kadaiTask.getId());

    Task claimedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(claimedKadaiTask.getState()).isEqualTo(TaskState.CLAIMED);
    assertThat(claimedKadaiTask.getOwner()).isEqualTo("admin");
    camunda8TestUtil.waitUntil(
        () -> "admin".equals(camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey)));
  }

  private void cancelCamundaTaskClaimViaRestrictedOutboundConnector(Task kadaiTask)
      throws Exception {
    long camundaTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());

    kadaiEngine.getTaskService().cancelClaim(kadaiTask.getId());

    Task readyKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(readyKadaiTask.getState()).isEqualTo(TaskState.READY);
    camunda8TestUtil.waitUntil(
        () -> camunda8TestUtil.getCamundaTaskAssignee(camundaTaskKey) == null);
  }

  private void completeCamundaTaskViaRestrictedOutboundConnector(
      CamundaClient restrictedClient, Task kadaiTask) throws Exception {
    final long camundaTaskKey = extractUserTaskKeyFromTaskId(kadaiTask.getExternalId());

    kadaiEngine.getTaskService().claim(kadaiTask.getId());
    kadaiEngine.getTaskService().completeTask(kadaiTask.getId());

    camunda8TestUtil.waitUntil(
        () ->
            handleNextJob(
                restrictedClient,
                "kadai-adapter-complete",
                DEFAULT_TENANT,
                USER_TASK_COMPLETED_JOB_WORKER_TYPE,
                userTaskCompletion::receiveTaskCompletedEvent));

    Task completedKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(completedKadaiTask.getState()).isEqualTo(TaskState.COMPLETED);
    camunda8TestUtil.waitUntil(
        () -> "COMPLETED".equals(camunda8TestUtil.getCamundaTaskStatus(camundaTaskKey)));
  }

  private void cancelProcessAndKadaiTaskViaRestrictedCancelledListener(
      CamundaClient adminClient, CamundaClient restrictedClient, Task kadaiTask) throws Exception {
    adminClient
        .newCancelInstanceCommand(Long.parseLong(kadaiTask.getBusinessProcessId()))
        .send()
        .join();

    camunda8TestUtil.waitUntil(
        () ->
            handleNextJob(
                restrictedClient,
                "kadai-adapter-cancel",
                DEFAULT_TENANT,
                USER_TASK_CANCELLED_JOB_WORKER_TYPE,
                userTaskCancellation::receiveTaskCancelledEvent));

    camunda8TestUtil.waitUntil(
        () ->
            kadaiEngine.getTaskService().getTask(kadaiTask.getId()).getState()
                == TaskState.CANCELLED);

    Task cancelledKadaiTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
    assertThat(cancelledKadaiTask.getState()).isEqualTo(TaskState.CANCELLED);
  }

  private Task getOnlyKadaiTaskByProcessInstance(ProcessInstanceEvent processInstance) {
    List<Task> tasks = getKadaiTasksByProcessInstance(processInstance);
    assertThat(tasks).hasSize(1);
    return tasks.get(0);
  }

  private List<Task> getKadaiTasksByProcessInstance(ProcessInstanceEvent processInstance) {
    String processInstanceKey = String.valueOf(processInstance.getProcessInstanceKey());
    return kadaiEngine.getTaskService().createTaskQuery().list().stream()
        .map(taskSummary -> getKadaiTask(taskSummary.getId()))
        .filter(task -> processInstanceKey.equals(task.getBusinessProcessId()))
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
