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

import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.resource.Camunda7TaskEventListResource;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.util.List;
import org.json.JSONException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/** Class to assist with building requests against the KADAI Outbox REST API. */
public class KadaiOutboxRequester {

  private static final String BASIC_OUTBOX_PATH = "http://localhost:10020/outbox-rest/events";

  private final RestClient restClient;

  private final HttpHeaderProvider httpHeaderProvider;

  public KadaiOutboxRequester(RestClient restClient, HttpHeaderProvider httpHeaderProvider) {
    this.restClient = restClient;
    this.httpHeaderProvider = httpHeaderProvider;
  }

  public boolean deleteFailedEvent(int id) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    ResponseEntity<String> answer =
        restClient
            .delete()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public boolean deleteAllFailedEvents() throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/delete-failed-events";

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    ResponseEntity<String> answer =
        restClient
            .post()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body("{}")
            .retrieve()
            .toEntity(String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public List<Camunda7TaskEvent> getFailedEvents() {

    String url = BASIC_OUTBOX_PATH + "?retries=0";

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    ResponseEntity<Camunda7TaskEventListResource> answer =
        restClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(Camunda7TaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public List<Camunda7TaskEvent> getAllEvents() {

    String url = BASIC_OUTBOX_PATH;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    ResponseEntity<Camunda7TaskEventListResource> answer =
        restClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(Camunda7TaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public boolean setRemainingRetries(int id, int newRetries) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    String body = "{\"remainingRetries\":" + newRetries + "}";
    ResponseEntity<String> answer =
        restClient
            .patch()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(body)
            .retrieve()
            .toEntity(String.class);

    if (HttpStatus.OK.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public boolean setRemainingRetriesForAll(int newRetries) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "?retries=0";

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    String body = "{\"remainingRetries\":" + newRetries + "}";
    ResponseEntity<String> answer =
        restClient
            .patch()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(body)
            .retrieve()
            .toEntity(String.class);

    if (HttpStatus.OK.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }
}
