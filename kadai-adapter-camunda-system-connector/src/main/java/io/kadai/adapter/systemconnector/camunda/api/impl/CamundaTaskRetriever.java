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
import io.kadai.adapter.camunda.outbox.rest.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.CamundaTaskEventListResource;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Retrieves new tasks from camunda that have been started or finished by camunda. */
@Component
public class CamundaTaskRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

  private final HttpHeaderProvider httpHeaderProvider;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  public CamundaTaskRetriever(
      HttpHeaderProvider httpHeaderProvider, ObjectMapper objectMapper, RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.objectMapper = objectMapper;
    this.restTemplate = restTemplate;
  }

  public List<ReferencedTask> retrieveNewStartedCamundaTasks(
      String camundaSystemTaskEventUrl,
      String camundaSystemEngineIdentifier,
      Duration lockDuration) {

    LOGGER.debug("entry to retrieveNewStartedCamundaTasks.");

    List<CamundaTaskEvent> camundaTaskEvents =
        getCamundaTaskEvents(
            camundaSystemTaskEventUrl,
            CamundaSystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS,
            lockDuration);

    List<ReferencedTask> referencedTasks =
        getReferencedTasksFromCamundaTaskEvents(camundaTaskEvents, camundaSystemEngineIdentifier);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from retrieveActiveCamundaTasks. Retrieved Tasks: {}", referencedTasks);
    }
    return referencedTasks;
  }

  public void retrieveCsrfToken(String camundaSystemTaskEventUrl) throws Exception {
    LOGGER.debug("entry to retrieveCsrfToken.");

    String requestUrl = camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_GET_CSRF_TOKEN;

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
    HttpHeaders responseHeaders = responseEntity.getHeaders();
    List<String> cookies = responseHeaders.get(HttpHeaders.SET_COOKIE);

    if (cookies == null || cookies.isEmpty()) {
      throw new Exception("No cookies found in the response.");
    }

    String xsrfToken;

    for (String cookie : cookies) {
      if (cookie.startsWith("XSRF-TOKEN=")) {
        xsrfToken = cookie.split(";")[0].substring("XSRF-TOKEN=".length());
        httpHeaderProvider.setCsrfToken(xsrfToken);
        LOGGER.info("Received XSRF-TOKEN: {}", xsrfToken);
        return;
      }
    }

    throw new Exception("XSRF-TOKEN cookie not found in the response.");
  }

  public List<ReferencedTask> retrieveFinishedCamundaTasks(
      String camundaSystemUrl, String camundaSystemEngineIdentifier, Duration lockDuration) {
    LOGGER.debug("entry to retrieveFinishedCamundaTasks. CamundSystemURL = {} ", camundaSystemUrl);

    List<CamundaTaskEvent> camundaTaskEvents =
        getCamundaTaskEvents(
            camundaSystemUrl,
            CamundaSystemConnectorImpl.URL_GET_CAMUNDA_FINISHED_EVENTS,
            lockDuration);

    List<ReferencedTask> referencedTasks =
        getReferencedTasksFromCamundaTaskEvents(camundaTaskEvents, camundaSystemEngineIdentifier);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from retrieveFinishedCamundaTasks. Retrieved Tasks: {}", referencedTasks);
    }
    return referencedTasks;
  }

  private List<CamundaTaskEvent> getCamundaTaskEvents(
      String camundaSystemTaskEventUrl, String eventSelector, Duration lockDuration) {

    String durationParameter = lockDuration == null ? "" : "&lock-for=" + lockDuration.toSeconds();
    String requestUrl = camundaSystemTaskEventUrl + eventSelector + durationParameter;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    LOGGER.debug(
        "retrieving camunda task event resources with url {} and headers {}", requestUrl, headers);

    CamundaTaskEventListResource camundaTaskEventListResource = new CamundaTaskEventListResource();
    camundaTaskEventListResource.setCamundaTaskEvents(new ArrayList<>());

    try {

      ResponseEntity<CamundaTaskEventListResource> responseEntity =
          restTemplate.exchange(
              requestUrl,
              HttpMethod.GET,
              new HttpEntity<>(headers),
              CamundaTaskEventListResource.class);

      camundaTaskEventListResource = responseEntity.getBody();

      List<CamundaTaskEvent> retrievedEvents = camundaTaskEventListResource.getCamundaTaskEvents();

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

  private List<ReferencedTask> getReferencedTasksFromCamundaTaskEvents(
      List<CamundaTaskEvent> camundaTaskEvents, String systemEngineIdentifier) {

    List<ReferencedTask> referencedTasks = new ArrayList<>();

    for (CamundaTaskEvent camundaTaskEvent : camundaTaskEvents) {

      if (systemEngineIdentifier == null
          || Objects.equals(camundaTaskEvent.getSystemEngineIdentifier(), systemEngineIdentifier)) {

        String referencedTaskJson = camundaTaskEvent.getPayload();

        try {

          ReferencedTask referencedTask =
              objectMapper.readValue(referencedTaskJson, ReferencedTask.class);
          referencedTask.setOutboxEventId(String.valueOf(camundaTaskEvent.getId()));
          referencedTask.setOutboxEventType(String.valueOf(camundaTaskEvent.getType()));
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
