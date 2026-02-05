package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
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
import org.junit.jupiter.api.ClassOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
@TestClassOrder(OrderAnnotation.class)
class UserTaskCancellationTest {

  @Nested
  @Order(1)
  @KadaiAdapterCamunda8SpringBootTest
  class NoMultiTenancyUserTaskCancellationTest {
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

      Thread.sleep(100);

      final Task canceledTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(canceledTask.getState()).isEqualTo(TaskState.CANCELLED);
    }
  }

  @Nested
  @Order(2)
  @TestPropertySource("classpath:camunda8-mt-test-application.properties")
  @KadaiAdapterCamunda8SpringBootTest
  class MultiTenancyUserTaskCancellationTest {

    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CancelKadaiTask_When_CamundaTaskIsCancelled() throws Exception {
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

      client.newCancelInstanceCommand(processInstance.getProcessInstanceKey()).send().join();

      Thread.sleep(100);

      final Task canceledTask = kadaiEngine.getTaskService().getTask(kadaiTask.getId());
      assertThat(canceledTask.getState()).isEqualTo(TaskState.CANCELLED);

      CamundaAssert.assertThat(processInstance).isTerminated();
    }
  }

  @Nested
  @Order(3)
  @TestPropertySource(
      locations = "classpath:camunda8-mt-test-application.properties",
      properties = {"camunda.client.worker.defaults.tenant-ids=tenant1,tenant2,<default>"})
  @KadaiAdapterCamunda8SpringBootTest
  class MultiTenancyUserTaskCancellationIsolationTest {

    @Autowired private CamundaClient client;
    @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
    @Autowired private KadaiEngine kadaiEngine;

    @Test
    @WithAccessId(user = "admin")
    void should_CancelKadaiTaskForSpecificTenantButNotCivilian_When_CamundaTaskIsCancelled()
        throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");

      final String allTenantsUser = "demo";
      final String tenant1 = "tenant1";
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
          .tenantId(tenant1)
          .send()
          .join();
      final ProcessInstanceEvent processInstance1 =
          client
              .newCreateInstanceCommand()
              .bpmnProcessId("Test_Process")
              .latestVersion()
              .tenantId(tenant1)
              .send()
              .join();
      CamundaAssert.assertThat(processInstance1).isActive();

      final String tenant2 = "tenant2";
      client.newCreateTenantCommand().tenantId(tenant2).name(tenant2).send().join();
      client
          .newAssignUserToTenantCommand()
          .username(allTenantsUser)
          .tenantId(tenant2)
          .send()
          .join();
      client
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .tenantId(tenant2)
          .send()
          .join();
      final ProcessInstanceEvent processInstance2 =
          client
              .newCreateInstanceCommand()
              .bpmnProcessId("Test_Process")
              .latestVersion()
              .tenantId(tenant2)
              .send()
              .join();
      CamundaAssert.assertThat(processInstance2).isActive();

      final List<TaskSummary> tasks = kadaiEngine.getTaskService().createTaskQuery().list();
      assertThat(tasks).hasSize(2);
      assertThat(tasks.get(0).getState()).isEqualTo(TaskState.READY);
      assertThat(tasks.get(1).getState()).isEqualTo(TaskState.READY);

      final Task kadaiTask1 = kadaiEngine.getTaskService().getTask(tasks.get(0).getId());
      client.newCancelInstanceCommand(processInstance1.getProcessInstanceKey()).send().join();
      Thread.sleep(100);
      final Task canceledTask = kadaiEngine.getTaskService().getTask(kadaiTask1.getId());
      assertThat(canceledTask.getState()).isEqualTo(TaskState.CANCELLED);
      CamundaAssert.assertThat(processInstance1).isTerminated();

      final Task kadaiTask2 = kadaiEngine.getTaskService().getTask(tasks.get(1).getId());
      assertThat(kadaiTask2.getState()).isEqualTo(TaskState.READY);
      CamundaAssert.assertThat(processInstance2).isActive();
    }
  }
}
