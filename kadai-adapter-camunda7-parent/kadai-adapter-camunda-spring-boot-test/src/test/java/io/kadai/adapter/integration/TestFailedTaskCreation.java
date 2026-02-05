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

import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.impl.configuration.DbCleaner;
import io.kadai.impl.configuration.DbCleaner.ApplicationDatabaseType;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

/** Test class to test failed task creation scenarios from camunda to KADAI. */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestFailedTaskCreation extends AbsIntegrationTest {

  @AfterEach
  @WithAccessId(user = "taskadmin")
  void resetOutbox() {
    DbCleaner cleaner = new DbCleaner();
    cleaner.clearDb(camundaBpmDataSource, ApplicationDatabaseType.OUTBOX);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CountDownRetriesAndAddToFailedEvents_When_TaskCreationFailedInKadai()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retries still above 0

    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).isEmpty();

    // adapter makes retries
    Thread.sleep(this.adapterRetryAndBlockingInterval);

    failedEvents = kadaiOutboxRequester.getFailedEvents();
    // retries = 0, no retries left
    assertThat(failedEvents).hasSize(3);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_LogError_When_TaskCreationFailedInKadai() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();

    // retries still above 0
    assertThat(failedEvents).isEmpty();

    // adapter makes retries
    Thread.sleep(this.adapterRetryAndBlockingInterval);

    failedEvents = kadaiOutboxRequester.getFailedEvents();
    // retries = 0, no retries left

    assertThat(failedEvents).hasSize(3);

    assertThat(failedEvents)
        .extracting(Camunda7TaskEvent::getCamundaTaskId)
        .containsExactlyInAnyOrderElementsOf(camundaTaskIds);

    assertThat(failedEvents)
        .extracting(Camunda7TaskEvent::getError)
        .allMatch(
            error ->
                error.contains(
                    "{\"name\":"
                        + "\"io.kadai.workbasket.api.exceptions.WorkbasketNotFoundException\","
                        + "\"message\":"
                        + "\"Workbasket with key 'invalidWorkbasketKey' "
                        + "and domain 'null' was not found.\"}"));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_DeleteFailedEvent_When_CallingDeleteEndpoint() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // adapter makes retries
    Thread.sleep(
        (long) (this.adapterTaskPollingInterval * 1.2 + this.adapterRetryAndBlockingInterval));

    // retries = 0, no retries left
    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).hasSize(3);

    boolean eventDeleted = kadaiOutboxRequester.deleteFailedEvent(failedEvents.get(0).getId());

    assertThat(eventDeleted).isTrue();

    Assertions.assertThat(kadaiOutboxRequester.getFailedEvents()).hasSize(2);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_DeleteAllFailedEvents_When_CallingDeleteAllFailedEndpoint() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep(
        (long) (this.adapterTaskPollingInterval * 1.2 + this.adapterRetryAndBlockingInterval));

    // retries = 0, no retries left

    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();
    assertThat(failedEvents).hasSize(3);

    boolean eventsDeleted = kadaiOutboxRequester.deleteAllFailedEvents();

    assertThat(eventsDeleted).isTrue();

    failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).isEmpty();
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetRetryForFailedEvent_When_CallingSetRetriesEndpoint() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep(
        (long) (this.adapterTaskPollingInterval * 1.2 + this.adapterRetryAndBlockingInterval));

    // retries = 0, no retries left
    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).hasSize(3);

    // reset specific failedEvent
    boolean remainingRetriesSet =
        kadaiOutboxRequester.setRemainingRetries(failedEvents.get(0).getId(), 3);

    assertThat(remainingRetriesSet).isTrue();

    failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).hasSize(2);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetRetryForAllFailedEvents_When_CallingSetRetriesForAllFailedEndpoint()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep(
        (long) (this.adapterTaskPollingInterval * 1.2 + this.adapterRetryAndBlockingInterval));

    // retries = 0, no retries left
    List<Camunda7TaskEvent> failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).hasSize(3);

    // reset specific failedEvent
    boolean remainingRetriesSet = kadaiOutboxRequester.setRemainingRetriesForAll(3);

    assertThat(remainingRetriesSet).isTrue();

    failedEvents = kadaiOutboxRequester.getFailedEvents();

    assertThat(failedEvents).isEmpty();
  }
}
