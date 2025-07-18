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

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.models.TaskSummary;
import java.time.Instant;
import java.util.List;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

/** Test class to test the completion of camunda tasks upon termination of kadai tasks. */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestTaskTermination extends AbsIntegrationTest {

  @Autowired private JobExecutor jobExecutor;
  @Autowired private KadaiTaskTerminator kadaiTaskTerminator;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteCamundaTask_When_TerminatingKadaiTask() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);

      taskService.terminateTask(kadaiTasks.get(0).getId());

      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));

      // check if camunda task got completed and therefore doesn't exist anymore
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();
    }
    Instant lastRunTime = kadaiTaskTerminator.getLastSchedulerRun().getRunTime();
    assertThat(lastRunTime).isNotNull();
    assertThat(lastRunTime).isAfter(Instant.now().minusSeconds(5));
  }
}
