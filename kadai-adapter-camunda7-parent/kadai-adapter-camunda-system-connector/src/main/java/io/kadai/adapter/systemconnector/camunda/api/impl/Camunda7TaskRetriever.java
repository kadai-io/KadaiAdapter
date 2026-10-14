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

import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEventListResource;
import io.kadai.adapter.systemconnector.api.InboundReferencedTask;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.dto.Camunda7ReferencedTaskDto;
import io.kadai.adapter.systemconnector.camunda.mapper.Camunda7ReferencedTaskMapper;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Retrieves new tasks from camunda that have been started or finished by camunda. */
@Component
public class Camunda7TaskRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda7TaskRetriever.class);

  private final HttpHeaderProvider httpHeaderProvider;
  private final JsonMapper jsonMapper;
  private final RestClient restClient;
  private final Camunda7ReferencedTaskMapper referencedTaskMapper;

  public Camunda7TaskRetriever(
      HttpHeaderProvider httpHeaderProvider,
      JsonMapper jsonMapper,
      RestClient restClient,
      Camunda7ReferencedTaskMapper referencedTaskMapper) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.jsonMapper = jsonMapper;
    this.restClient = restClient;
    this.referencedTaskMapper = referencedTaskMapper;
  }

  public List<InboundReferencedTask> retrieveNewStartedCamunda7Tasks(
      String camundaSystemTaskEventUrl,
      String camundaSystemEngineIdentifier,
      Duration lockDuration) {

    List<Camunda7TaskEvent> camunda7TaskEvents =
        getCamunda7TaskEvents(
            camundaSystemTaskEventUrl,
            Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS,
            lockDuration);

    List<InboundReferencedTask> referencedTasks =
        getReferencedTasksFromCamunda7TaskEvents(camunda7TaskEvents, camundaSystemEngineIdentifier);

    return referencedTasks;
  }

  public List<InboundReferencedTask> retrieveFinishedCamunda7Tasks(
      String camundaSystemUrl, String camundaSystemEngineIdentifier, Duration lockDuration) {
    List<Camunda7TaskEvent> camunda7TaskEvents =
        getCamunda7TaskEvents(
            camundaSystemUrl,
            Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_FINISHED_EVENTS,
            lockDuration);

    List<InboundReferencedTask> referencedTasks =
        getReferencedTasksFromCamunda7TaskEvents(camunda7TaskEvents, camundaSystemEngineIdentifier);

    return referencedTasks;
  }

  private List<Camunda7TaskEvent> getCamunda7TaskEvents(
      String camundaSystemTaskEventUrl, String eventSelector, Duration lockDuration) {

    String durationParameter = lockDuration == null ? "" : "&lock-for=" + lockDuration.toSeconds();
    String requestUrl = camundaSystemTaskEventUrl + eventSelector + durationParameter;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

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

      return retrievedEvents;

    } catch (Exception e) {
      LOGGER.error(
          "Caught exception while trying to retrieve CamundaTaskEvents from system with URL "
              + camundaSystemTaskEventUrl,
          e);
    }

    return Collections.emptyList();
  }

  private List<InboundReferencedTask> getReferencedTasksFromCamunda7TaskEvents(
      List<Camunda7TaskEvent> camunda7TaskEvents, String systemEngineIdentifier) {

    List<InboundReferencedTask> referencedTasks = new ArrayList<>();
    int filteredCount = 0;
    int malformedCount = 0;

    for (Camunda7TaskEvent camunda7TaskEvent : camunda7TaskEvents) {

      if (systemEngineIdentifier == null
          || Objects.equals(
              camunda7TaskEvent.getSystemEngineIdentifier(), systemEngineIdentifier)) {

        String referencedTaskJson = camunda7TaskEvent.getPayload();

        try {

          Camunda7ReferencedTaskDto dto =
              jsonMapper.readValue(referencedTaskJson, Camunda7ReferencedTaskDto.class);
          ReferencedTask referencedTask = referencedTaskMapper.toReferencedTask(dto);
          referencedTasks.add(
              new Camunda7InboundReferencedTask(referencedTask, camunda7TaskEvent.getId()));

        } catch (JacksonException | IllegalArgumentException e) {
          malformedCount++;
          LOGGER.warn(
              "Could not map Camunda 7 task event {}: {}",
              camunda7TaskEvent.getId(),
              e.getClass().getSimpleName());
        }
      } else {
        filteredCount++;
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Mapped {} of {} Camunda 7 outbox events for engine {} (filtered: {}, malformed: {})",
          referencedTasks.size(),
          camunda7TaskEvents.size(),
          systemEngineIdentifier,
          filteredCount,
          malformedCount);
    }
    return referencedTasks;
  }
}
