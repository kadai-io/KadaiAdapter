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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEventListResource;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Retrieves new tasks from camunda that have been started or finished by camunda. */
@Component
public class Camunda7TaskRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda7TaskRetriever.class);

  private final HttpHeaderProvider httpHeaderProvider;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public Camunda7TaskRetriever(
      HttpHeaderProvider httpHeaderProvider, ObjectMapper objectMapper, RestClient restClient) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.objectMapper = objectMapper;
    this.restClient = restClient;
  }

  public List<ReferencedTask> retrieveNewStartedCamunda7Tasks(
      String camundaSystemTaskEventUrl,
      String camundaSystemEngineIdentifier,
      Duration lockDuration) {

    LOGGER.debug("entry to retrieveNewStartedCamundaTasks.");

    List<Camunda7TaskEvent> camunda7TaskEvents =
        getCamunda7TaskEvents(
            camundaSystemTaskEventUrl,
            Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS,
            lockDuration);

    List<ReferencedTask> referencedTasks =
        getReferencedTasksFromCamunda7TaskEvents(camunda7TaskEvents, camundaSystemEngineIdentifier);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from retrieveActiveCamundaTasks. Retrieved Tasks: {}", referencedTasks);
    }
    return referencedTasks;
  }

  public List<ReferencedTask> retrieveFinishedCamunda7Tasks(
      String camundaSystemUrl, String camundaSystemEngineIdentifier, Duration lockDuration) {
    LOGGER.debug("entry to retrieveFinishedCamundaTasks. CamundaSystemURL = {} ", camundaSystemUrl);

    List<Camunda7TaskEvent> camunda7TaskEvents =
        getCamunda7TaskEvents(
            camundaSystemUrl,
            Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_FINISHED_EVENTS,
            lockDuration);

    List<ReferencedTask> referencedTasks =
        getReferencedTasksFromCamunda7TaskEvents(camunda7TaskEvents, camundaSystemEngineIdentifier);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from retrieveFinishedCamundaTasks. Retrieved Tasks: {}", referencedTasks);
    }
    return referencedTasks;
  }

  private List<Camunda7TaskEvent> getCamunda7TaskEvents(
      String camundaSystemTaskEventUrl, String eventSelector, Duration lockDuration) {

    String durationParameter = lockDuration == null ? "" : "&lock-for=" + lockDuration.toSeconds();
    String requestUrl = camundaSystemTaskEventUrl + eventSelector + durationParameter;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    LOGGER.debug(
        "retrieving camunda task event resources with url {} and headers {}", requestUrl, headers);

    Camunda7TaskEventListResource camunda7TaskEventListResource =
        new Camunda7TaskEventListResource();
    camunda7TaskEventListResource.setCamunda7TaskEvents(new ArrayList<>());

    try {

      camunda7TaskEventListResource =
          restClient
              .get()
              .uri(requestUrl)
              .headers(httpHeaders -> httpHeaders.addAll(headers))
              .retrieve()
              .toEntity(Camunda7TaskEventListResource.class)
              .getBody();

      List<Camunda7TaskEvent> retrievedEvents =
          camunda7TaskEventListResource.getCamunda7TaskEvents();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("retrieved camunda task events {}", retrievedEvents);
      }

      return retrievedEvents;

    } catch (Exception e) {
      LOGGER.error(
          "Caught exception while trying to retrieve CamundaTaskEvents from system with URL "
              + camundaSystemTaskEventUrl,
          e);
    }

    return Collections.emptyList();
  }

  private List<ReferencedTask> getReferencedTasksFromCamunda7TaskEvents(
      List<Camunda7TaskEvent> camunda7TaskEvents, String systemEngineIdentifier) {

    List<ReferencedTask> referencedTasks = new ArrayList<>();

    for (Camunda7TaskEvent camunda7TaskEvent : camunda7TaskEvents) {

      if (systemEngineIdentifier == null
          || Objects.equals(
              camunda7TaskEvent.getSystemEngineIdentifier(), systemEngineIdentifier)) {

        String referencedTaskJson = camunda7TaskEvent.getPayload();

        try {

          ReferencedTask referencedTask =
              objectMapper.readValue(referencedTaskJson, ReferencedTask.class);
          referencedTask.setOutboxEventId(String.valueOf(camunda7TaskEvent.getId()));
          referencedTask.setOutboxEventType(String.valueOf(camunda7TaskEvent.getType()));
          referencedTasks.add(referencedTask);

        } catch (IOException e) {

          LOGGER.warn(
              "Caught {} while trying to create ReferencedTasks "
                  + " out of CamundaTaskEventResources. RefTaskJson = {}",
              e,
              referencedTaskJson);
        }
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("retrieved reference tasks {}", referencedTasks);
      }
    }
    return referencedTasks;
  }
}
