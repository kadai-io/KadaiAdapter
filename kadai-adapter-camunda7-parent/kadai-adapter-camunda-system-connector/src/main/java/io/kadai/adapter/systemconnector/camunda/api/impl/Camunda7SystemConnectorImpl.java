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

package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.InboundSystemConnector;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import java.time.Duration;
import java.util.List;

/** Sample Implementation of SystemConnector. */
public class Camunda7SystemConnectorImpl
    implements InboundSystemConnector, OutboundSystemConnector {

  static final String URL_GET_CAMUNDA_TASKS = "/task/";

  static final String URL_GET_CAMUNDA_CREATE_EVENTS = "/events?type=create";
  static final String URL_GET_CAMUNDA_FINISHED_EVENTS = "/events?type=complete&type=delete";
  static final String URL_DELETE_CAMUNDA_EVENTS = "/events/delete-successful-events";
  static final String URL_CAMUNDA_EVENT_DECREASE_REMAINING_RETRIES =
      "/events/%d/decrease-remaining-retries";
  static final String URL_CAMUNDA_UNLOCK_EVENT = "/events/unlock-event/%d";
  static final String BODY_SET_CAMUNDA_VARIABLES = "{\"variables\":{";
  static final String LOCAL_VARIABLE_PATH = "/localVariables";
  static final String EMPTY_REQUEST_BODY = "{}";

  static final String COMPLETE_TASK = "/complete";
  static final String SET_ASSIGNEE = "/assignee";
  static final String BODY_SET_ASSIGNEE = "{\"userId\":";
  static final String UNCLAIM_TASK = "/unclaim";

  private final Camunda7System camunda7SystemUrl;
  private final Camunda7TaskRetriever taskRetriever;
  private final Camunda7TaskCompleter taskCompleter;
  private final Camunda7TaskClaimer taskClaimer;
  private final Camunda7TaskClaimCanceler taskClaimCanceler;
  private final Camunda7TaskEventCleaner taskEventCleaner;
  private final Camunda7TaskEventErrorHandler taskEventErrorHandler;
  private final Duration lockDuration;

  public Camunda7SystemConnectorImpl(Camunda7System camunda7SystemUrl) {
    this.camunda7SystemUrl = camunda7SystemUrl;
    taskRetriever = AdapterSpringContextProvider.getBean(Camunda7TaskRetriever.class);
    taskCompleter = AdapterSpringContextProvider.getBean(Camunda7TaskCompleter.class);
    taskClaimer = AdapterSpringContextProvider.getBean(Camunda7TaskClaimer.class);
    taskClaimCanceler = AdapterSpringContextProvider.getBean(Camunda7TaskClaimCanceler.class);
    taskEventCleaner = AdapterSpringContextProvider.getBean(Camunda7TaskEventCleaner.class);
    taskEventErrorHandler =
        AdapterSpringContextProvider.getBean(Camunda7TaskEventErrorHandler.class);
    lockDuration =
        Duration.ofSeconds(
            AdapterSpringContextProvider.getBean(Camunda7SystemConnectorConfiguration.class)
                .getLockDuration());
  }

  @Override
  public List<ReferencedTask> retrieveNewStartedReferencedTasks() {
    return taskRetriever.retrieveNewStartedCamunda7Tasks(
        camunda7SystemUrl.getSystemTaskEventUrl(),
        camunda7SystemUrl.getCamunda7EngineIdentifier(),
        lockDuration);
  }

  @Override
  public void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camunda7SystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public List<ReferencedTask> retrieveFinishedReferencedTasks() {
    return taskRetriever.retrieveFinishedCamunda7Tasks(
        camunda7SystemUrl.getSystemTaskEventUrl(),
        camunda7SystemUrl.getCamunda7EngineIdentifier(),
        lockDuration);
  }

  @Override
  public void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camunda7SystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public String retrieveReferencedTaskVariables(String taskId) {
    return null;
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
    return taskCompleter.completeCamunda7Task(camunda7SystemUrl, camundaTask);
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimer.claimCamunda7Task(camunda7SystemUrl, camundaTask);
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimCanceler.cancelClaimOfCamunda7Task(camunda7SystemUrl, camundaTask);
  }

  @Override
  public String getSystemUrl() {
    return camunda7SystemUrl.getSystemRestUrl();
  }

  @Override
  public void kadaiTaskFailedToBeCreatedForNewReferencedTask(
      ReferencedTask referencedTask, Exception e) {
    taskEventErrorHandler.decreaseRemainingRetriesAndLogErrorForReferencedTask(
        referencedTask, e, camunda7SystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public void unlockEvent(String eventId) {
    taskEventErrorHandler.unlockEvent(eventId, camunda7SystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public String toString() {
    return "Camunda7SystemConnectorImpl [camundaSystemUrl=" + camunda7SystemUrl + "]";
  }
}
