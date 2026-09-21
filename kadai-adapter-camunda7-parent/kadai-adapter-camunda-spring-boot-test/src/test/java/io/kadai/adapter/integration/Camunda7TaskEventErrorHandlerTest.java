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
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.InboundReferencedTask;
import io.kadai.adapter.systemconnector.api.InboundSystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7InboundReferencedTask;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.impl.configuration.DbCleaner;
import io.kadai.impl.configuration.DbCleaner.ApplicationDatabaseType;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
class Camunda7TaskEventErrorHandlerTest extends AbsIntegrationTest {

  @Autowired private AdapterManager adapterManager;

  @BeforeEach
  void init() {
    adapterManager.init();
  }

  @AfterEach
  @WithAccessId(user = "taskadmin")
  void increaseCounter() {
    DbCleaner cleaner = new DbCleaner();
    cleaner.clearDb(camundaBpmDataSource, ApplicationDatabaseType.OUTBOX);
  }

  @Test
  void should_CreateErrorLogWithOneCause_When_ExceptionWithOneCauseOccurred() {
    Exception testException = new NumberFormatException("exception");
    Exception testCause = new NumberFormatException("cause");
    testException.initCause(testCause);
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testCause.getClass().getName())
                            .put("message", testCause.getMessage())));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    List<InboundReferencedTask> referencedTasks =
        this.adapterManager.getInboundSystemConnectors().entrySet().stream()
            .flatMap(
                entry -> {
                  InboundSystemConnector connector = entry.getValue();
                  return connector.retrieveNewStartedReferencedTasks().stream()
                      .peek(
                          task ->
                              connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                  task, testException));
                })
            .toList();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Camunda7TaskEvent camunda7TaskEvent = getAnEventWithError(referencedTasks);
    JSONObject errorJson = new JSONObject(camunda7TaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CutErrorLog_When_ExceptionCauseTreeIsTooLong() {
    final Exception testException = new NumberFormatException("exception");
    final Exception testCause = new NumberFormatException("cause");
    final Exception testCauseVeryLong = new NumberFormatException(StringUtils.repeat("x", 1000));
    testCause.initCause(testCauseVeryLong);
    testException.initCause(testCause);
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testCause.getClass().getName())
                            .put("message", testCause.getMessage()))
                    .put("..."));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");

    List<InboundReferencedTask> referencedTasks =
        this.adapterManager.getInboundSystemConnectors().entrySet().stream()
            .flatMap(
                entry -> {
                  InboundSystemConnector connector = entry.getValue();
                  return connector.retrieveNewStartedReferencedTasks().stream()
                      .peek(
                          task ->
                              connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                  task, testException));
                })
            .toList();

    Camunda7TaskEvent camunda7TaskEvent = getAnEventWithError(referencedTasks);
    JSONObject errorJson = new JSONObject(camunda7TaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CutErrorLogProperly_When_AddingDotDotDotToErrorLog() {
    final Exception testException =
        new Exception(
            "exception",
            new Exception(
                "cause",
                new Exception(
                    // We need an exception message with a length of 825 characters, so that the
                    // overall length of the output string is 999 characters. Adding "..." would
                    // yield into >1000, thus this exception should not be included in the errorLog
                    StringUtils.repeat("x", 825),
                    new NumberFormatException(StringUtils.repeat("x", 1000)))));
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testException.getCause().getClass().getName())
                            .put("message", testException.getCause().getMessage()))
                    .put("..."));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    List<InboundReferencedTask> referencedTasks =
        this.adapterManager.getInboundSystemConnectors().entrySet().stream()
            .flatMap(
                entry -> {
                  InboundSystemConnector connector = entry.getValue();
                  return connector.retrieveNewStartedReferencedTasks().stream()
                      .peek(
                          task ->
                              connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                  task, testException));
                })
            .toList();
    Camunda7TaskEvent camunda7TaskEvent = getAnEventWithError(referencedTasks);
    JSONObject errorJson = new JSONObject(camunda7TaskEvent.getError());
    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CreateErrorLogWithoutCause_When_ExceptionWithoutCauseOccurred() {
    Exception testException = new NumberFormatException("exception");
    JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put("cause", new JSONArray());

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    List<InboundReferencedTask> referencedTasks =
        this.adapterManager.getInboundSystemConnectors().entrySet().stream()
            .flatMap(
                entry -> {
                  InboundSystemConnector connector = entry.getValue();
                  return connector.retrieveNewStartedReferencedTasks().stream()
                      .peek(
                          task ->
                              connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                  task, testException));
                })
            .toList();
    Camunda7TaskEvent camunda7TaskEvent = getAnEventWithError(referencedTasks);
    JSONObject errorJson = new JSONObject(camunda7TaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_UnlockEvent_When_KadaiTaskCreationFails() {
    Exception testException = new NumberFormatException("exception");
    InboundSystemConnector connector =
        adapterManager.getInboundSystemConnectors().values().stream().findFirst().orElseThrow();

    camundaProcessengineRequester.startCamundaProcessAndReturnId("simple_user_task_process", "");
    InboundReferencedTask inboundTask =
        connector.retrieveNewStartedReferencedTasks().stream().findFirst().orElseThrow();
    int taskEventId = ((Camunda7InboundReferencedTask) inboundTask).getTaskEventId();
    Camunda7TaskEvent lockedEvent = getEventWithId(taskEventId);

    assertThat(lockedEvent.getLockExpiresAt()).isNotNull();
    final int remainingRetriesBeforeFailure = lockedEvent.getRemainingRetries();

    connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(inboundTask, testException);

    Camunda7TaskEvent eventAfterFailure = getEventWithId(taskEventId);
    JSONObject errorJson = new JSONObject(eventAfterFailure.getError());
    assertThat(errorJson.getJSONObject("exception").getString("name"))
        .isEqualTo(testException.getClass().getName());
    assertThat(errorJson.getJSONObject("exception").getString("message"))
        .isEqualTo(testException.getMessage());
    assertThat(eventAfterFailure.getRemainingRetries())
        .isEqualTo(remainingRetriesBeforeFailure - 1);
    assertThat(eventAfterFailure.getLockExpiresAt()).isNull();
  }

  @Test
  void should_UnlockAndKeepEvent_When_KadaiTaskTerminationFails() {
    Exception testException = new RuntimeException("exception");
    InboundSystemConnector connector =
        adapterManager.getInboundSystemConnectors().values().stream().findFirst().orElseThrow();

    String processInstanceId =
        camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId).getFirst();
    assertThat(camundaProcessengineRequester.completeTaskWithId(camundaTaskId)).isTrue();

    InboundReferencedTask inboundTask =
        connector.retrieveFinishedReferencedTasks().stream()
            .filter(task -> task.getReferencedTask().getId().equals(camundaTaskId))
            .findFirst()
            .orElseThrow();
    int taskEventId = ((Camunda7InboundReferencedTask) inboundTask).getTaskEventId();

    assertThat(getEventWithId(taskEventId).getLockExpiresAt()).isNotNull();

    connector.kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(inboundTask, testException);

    Camunda7TaskEvent eventAfterFailure = getEventWithId(taskEventId);
    assertThat(eventAfterFailure.getLockExpiresAt()).isNull();
  }

  private Camunda7TaskEvent getAnEventWithError(List<InboundReferencedTask> referencedTasks) {
    List<Camunda7TaskEvent> allEvents = kadaiOutboxRequester.getAllEvents();

    return allEvents.stream()
        .filter(
            event ->
                referencedTasks.stream()
                    .anyMatch(
                        task ->
                            event.getId()
                                == ((Camunda7InboundReferencedTask) task).getTaskEventId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No matching Camunda7TaskEvent found"));
  }

  private Camunda7TaskEvent getEventWithId(int taskEventId) {
    return kadaiOutboxRequester.getAllEvents().stream()
        .filter(event -> event.getId() == taskEventId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No matching Camunda7TaskEvent found"));
  }
}
