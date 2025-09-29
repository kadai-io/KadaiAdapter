package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.kadai.adapter.test.KadaiAdapterSpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.test.security.WithAccessId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CamundaSpringProcessTest
@KadaiAdapterSpringBootTest
class UserTaskCompletionTest {

  @Autowired private CamundaClient client;
  @Autowired private CamundaProcessTestContext processTestContext;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;

  @Test
  @WithAccessId(user = "admin")
  void should_CompleteProcessInstance() throws Exception {
    kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");

    // given: the processes are deployed
    client
        .newDeployResourceCommand()
        .addResourceFromClasspath("processes/sayHello.bpmn")
        .send()
        .join();

    // when
    final ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("Test_Process")
            .latestVersion()
            .send()
            .join();

    // then
    CamundaAssert.assertThat(processInstance).isCompleted();
  }
}
