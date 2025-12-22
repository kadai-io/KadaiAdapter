/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.integration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskClaimCanceler;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskClaimer;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration
@ExtendWith(JaasExtension.class)
class TestDisabledTaskClaim extends AbsIntegrationTest {

  @Autowired Camunda7TaskClaimer camunda7TaskClaimer;
  @Autowired Camunda7TaskClaimCanceler camunda7TaskClaimCanceler;

  @Value("${kadai.adapter.camunda.claiming.enabled}")
  private boolean claimingEnabled;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_NotClaimOrCancelClaimCamundaTask_When_CamundaClaimingDisabled() throws Exception {

    setClaimingEnabled(false);

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_assignee_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    // check that one new UserTask was started
    assertThat(camundaTaskIds).hasSize(1);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check external task id of created kadai task
    String camundaTaskId = camundaTaskIds.get(0);
    TaskSummary kadaiTask = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).single();

    String kadaiTaskExternalId = kadaiTask.getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);

    // verify that assignee is already set
    boolean assigneeAlreadySet =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeAlreadySet).isTrue();

    // verify that TaskState of kadai task is 'READY' first
    String kadaiTaskId = kadaiTask.getId();
    Task task = this.taskService.getTask(kadaiTaskId);
    assertThat(task.getState()).isEqualTo(TaskState.READY);

    // claim task in kadai and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(kadaiTaskId);
    Task updatedTask = this.taskService.getTask(kadaiTaskId);
    assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

    Thread.sleep((long) (this.adapterClaimPollingInterval * 1.3));
    // verify assignee for camunda task did not get updated
    boolean assigneeNotUpdated =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeNotUpdated).isTrue();

    // cancel claim KADAI task
    taskService.forceCancelClaim(task.getId());

    Thread.sleep((long) (this.adapterClaimPollingInterval * 1.3));

    // verify assignee for camunda task did not get updated
    assigneeNotUpdated =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeNotUpdated).isTrue();

    setClaimingEnabled(claimingEnabled);
  }

  private void setClaimingEnabled(boolean claimingEnbaled) throws Exception {
    Field claimerClaimingEnabled =
        camunda7TaskClaimer.getClass().getDeclaredField("claimingEnabled");
    claimerClaimingEnabled.setAccessible(true);
    claimerClaimingEnabled.setBoolean(camunda7TaskClaimer, claimingEnbaled);
    Field claimCancelerClaimingEnabled =
        camunda7TaskClaimCanceler.getClass().getDeclaredField("claimingEnabled");
    claimCancelerClaimingEnabled.setAccessible(true);
    claimCancelerClaimingEnabled.setBoolean(camunda7TaskClaimCanceler, claimingEnbaled);
  }
}
