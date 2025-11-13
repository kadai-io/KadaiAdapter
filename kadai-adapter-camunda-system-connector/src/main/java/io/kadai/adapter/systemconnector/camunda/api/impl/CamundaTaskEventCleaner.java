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

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Clears events in the Camunda outbox after the corresponding action has been carried out by KADAI.
 */
@Component
public class CamundaTaskEventCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventCleaner.class);
  private final HttpHeaderProvider httpHeaderProvider;
  private final RestClient restClient;

  public CamundaTaskEventCleaner(HttpHeaderProvider httpHeaderProvider, RestClient restClient) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restClient = restClient;
  }

  public void cleanEventsForReferencedTasks(
      List<ReferencedTask> referencedTasks, String camundaSystemTaskEventUrl)
      throws HttpStatusCodeException {

    LOGGER.debug(
        "entry to cleanEventsForReferencedTasks, CamundaSystemURL = {}", camundaSystemTaskEventUrl);

    String requestUrl =
        camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_DELETE_CAMUNDA_EVENTS;

    if (referencedTasks == null || referencedTasks.isEmpty()) {
      return;
    }

    String idsOfCamundaTaskEventsToDeleteFromOutbox =
        getIdsOfCamundaTaskEventsToDeleteFromOutbox(referencedTasks);
    LOGGER.debug("delete Events url {} ", requestUrl);

    deleteCamundaTaskEventsFromOutbox(requestUrl, idsOfCamundaTaskEventsToDeleteFromOutbox);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from cleanEventsForReferencedTasks.");
    }
  }

  private void deleteCamundaTaskEventsFromOutbox(
      String requestUrl, String idsOfCamundaTaskEventsToDeleteFromOutbox)
      throws HttpStatusCodeException {
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    restClient
        .post()
        .uri(requestUrl)
        .headers(httpHeaders -> httpHeaders.addAll(headers))
        .body(idsOfCamundaTaskEventsToDeleteFromOutbox)
        .retrieve()
        .toEntity(Void.class);
  }

  private String getIdsOfCamundaTaskEventsToDeleteFromOutbox(List<ReferencedTask> referencedTasks) {

    StringBuilder idsBuf = new StringBuilder();

    idsBuf.append("{\"taskCreationIds\":[");

    for (ReferencedTask referencedTask : referencedTasks) {
      idsBuf.append(referencedTask.getOutboxEventId().trim());
      idsBuf.append(',');
    }
    idsBuf.append("]}");
    return idsBuf.toString().replace(",]", "]");
  }
}
