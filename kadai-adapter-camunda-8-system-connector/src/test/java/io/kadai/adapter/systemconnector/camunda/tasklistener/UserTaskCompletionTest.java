package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static io.camunda.process.test.api.CamundaAssert.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8ConnectorTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@Disabled
@ContextConfiguration(classes = {Camunda8ConnectorTestConfiguration.class})
@SpringBootTest
@CamundaSpringProcessTest
class UserTaskCompletionTest {

  @Autowired private CamundaProcessTestContext processTestContext;
  @Autowired private CamundaClient client;

  private static final String PROCESS_NAME = "sayHello.bpmn";

  @BeforeEach
  void setup() {
    client.newDeployResourceCommand().addResourceFromClasspath(PROCESS_NAME).send().join();
  }

  @Test
  void testUserTaskCompletion() {
    // Given
    ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("Test_Process")
            .latestVersion()
            .send()
            .join();

    assertThat(processInstance).isActive(); // is failing for some reason

    // When
    // processTestContext.completeUserTask(byElementId("SayHello"));

    // Assert that the process instance has completed

    // todo: mock adapter to verify that the task was sent to the system connector
  }
}
