package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.kadai.adapter.test.KadaiAdapterSpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@CamundaSpringProcessTest
@KadaiAdapterSpringBootTest
@TestPropertySource("classpath:camunda8-test-application.properties")
class UserTaskCreationTest {

  @Autowired private CamundaClient client;
  @Autowired private CamundaProcessTestContext processTestContext;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;

  @Test
  @WithAccessId(user = "admin")
  void should_CreateKadaiTask() throws Exception {
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
    final List<TaskSummary> actual = kadaiEngine.getTaskService().createTaskQuery().list();
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).getState()).isEqualTo(TaskState.READY);
  }
}
