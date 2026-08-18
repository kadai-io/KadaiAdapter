package io.kadai.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.spring.properties.CamundaClientProperties;
import io.camunda.process.test.api.CamundaProcessTestContext;
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
class KadaiAdapterApplicationC8MultiTenancyIntegrationTest {

  private static final String PROCESS_ID = "Test_Process";
  private static final String TENANT_ID = "tenant1";
  private static final String SECOND_TENANT_ID = "tenant2";

  @Autowired private CamundaClient client;
  @Autowired private CamundaClientProperties clientProperties;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private CamundaProcessTestContext processTestContext;

  @Test
  @WithAccessId(user = "admin")
  void should_CreateTenantTask_When_MultiTenancyExampleApplicationIsConfigured() throws Exception {
    assertThat(clientProperties.getWorker().getDefaults().getTenantIds())
        .containsExactly("<default>", "tenant1", "tenant2");

    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
    kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
    registerTenant(TENANT_ID);
    registerTenant(SECOND_TENANT_ID);

    try (CamundaClient tenantClient =
        processTestContext.createClient(
            clientBuilder -> clientBuilder.defaultTenantId(TENANT_ID))) {
      tenantClient
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .tenantId(TENANT_ID)
          .send()
          .join();

      ProcessInstanceEvent processInstance =
          tenantClient
              .newCreateInstanceCommand()
              .bpmnProcessId(PROCESS_ID)
              .latestVersion()
              .tenantId(TENANT_ID)
              .send()
              .join();

      camunda8TestUtil.waitUntil(
          () ->
              getKadaiTasks(processInstance.getProcessInstanceKey()).stream()
                  .anyMatch(task -> task.getState() == TaskState.READY));

      assertThat(getKadaiTasks(processInstance.getProcessInstanceKey()))
          .singleElement()
          .satisfies(task -> assertThat(task.getState()).isEqualTo(TaskState.READY));
    }
  }

  private void registerTenant(String tenantId) {
    client.newCreateTenantCommand().tenantId(tenantId).name(tenantId).send().join();
    client
        .newAssignUserToTenantCommand()
        .username("demo")
        .tenantId(tenantId)
        .send()
        .join();
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
