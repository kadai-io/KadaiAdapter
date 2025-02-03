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
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import java.time.Duration;
import java.util.List;

/** Sample Implementation of SystemConnector. */
public class CamundaSystemConnectorImpl implements SystemConnector {

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
  static final String URL_GET_CSRF_TOKEN = "/events/csrf";

  private final CamundaSystemUrls.SystemUrlInfo camundaSystemUrl;

  private final CamundaTaskRetriever taskRetriever;

  private final CamundaTaskCompleter taskCompleter;

  private final CamundaTaskClaimer taskClaimer;

  private final CamundaTaskClaimCanceler taskClaimCanceler;

  private final CamundaTaskEventCleaner taskEventCleaner;

  private final CamundaTaskEventErrorHandler taskEventErrorHandler;

  private final Duration lockDuration;

  public CamundaSystemConnectorImpl(CamundaSystemUrls.SystemUrlInfo camundaSystemUrl) {
    this.camundaSystemUrl = camundaSystemUrl;
    taskRetriever = AdapterSpringContextProvider.getBean(CamundaTaskRetriever.class);
    taskCompleter = AdapterSpringContextProvider.getBean(CamundaTaskCompleter.class);
    taskClaimer = AdapterSpringContextProvider.getBean(CamundaTaskClaimer.class);
    taskClaimCanceler = AdapterSpringContextProvider.getBean(CamundaTaskClaimCanceler.class);
    taskEventCleaner = AdapterSpringContextProvider.getBean(CamundaTaskEventCleaner.class);
    taskEventErrorHandler =
        AdapterSpringContextProvider.getBean(CamundaTaskEventErrorHandler.class);
    lockDuration = AdapterSpringContextProvider.getBean(Duration.class);
  }

  @Override
  public List<ReferencedTask> retrieveNewStartedReferencedTasks() {
    return taskRetriever.retrieveNewStartedCamundaTasks(
        camundaSystemUrl.getSystemTaskEventUrl(),
        camundaSystemUrl.getCamundaEngineIdentifier(),
        lockDuration);
  }

  @Override
  public void retrieveCsrfToken() throws Exception {
    taskRetriever.retrieveCsrfToken(camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public List<ReferencedTask> retrieveFinishedReferencedTasks() {
    return taskRetriever.retrieveFinishedCamundaTasks(
        camundaSystemUrl.getSystemTaskEventUrl(),
        camundaSystemUrl.getCamundaEngineIdentifier(),
        lockDuration);
  }

  @Override
  public void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public String retrieveReferencedTaskVariables(String taskId) {
    return null;
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
    return taskCompleter.completeCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimer.claimCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimCanceler.cancelClaimOfCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public String getSystemUrl() {
    return camundaSystemUrl.getSystemRestUrl();
  }

  @Override
  public String getSystemIdentifier() {
    return camundaSystemUrl.getCamundaEngineIdentifier();
  }

  @Override
  public void kadaiTaskFailedToBeCreatedForNewReferencedTask(
      ReferencedTask referencedTask, Exception e) {
    taskEventErrorHandler.decreaseRemainingRetriesAndLogErrorForReferencedTask(
        referencedTask, e, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public void unlockEvent(String eventId) {
    taskEventErrorHandler.unlockEvent(eventId, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public String toString() {
    return "CamundaSystemConnectorImpl [camundaSystemUrl=" + camundaSystemUrl + "]";
  }
}
