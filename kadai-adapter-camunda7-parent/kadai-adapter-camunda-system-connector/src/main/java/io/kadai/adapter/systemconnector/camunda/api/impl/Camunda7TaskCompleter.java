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
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemUrls;
import io.kadai.common.api.exceptions.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/** Completes Camunda Tasks via the Camunda REST Api. */
public class Camunda7TaskCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda7TaskCompleter.class);

  private static final String COMPLETED_BY_KADAI_ADAPTER_LOCAL_VARIABLE = "completedByKadaiAdapter";
  private final HttpHeaderProvider httpHeaderProvider;
  private final RestClient restClient;

  public Camunda7TaskCompleter(HttpHeaderProvider httpHeaderProvider, RestClient restClient) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restClient = restClient;
  }

  public SystemResponse completeCamunda7Task(
      Camunda7SystemUrls.SystemUrlInfo camundaSystemUrlInfo, ReferencedTask referencedTask)
      throws HttpStatusCodeException {

    StringBuilder requestUrlBuilder = new StringBuilder();
    try {
      setAssigneeToOwnerOfReferencedTask(camundaSystemUrlInfo, referencedTask, requestUrlBuilder);
      setCompletionByKadaiAdapterAsLocalVariable(
          camundaSystemUrlInfo, referencedTask, requestUrlBuilder);

      return performCompletion(camundaSystemUrlInfo, referencedTask, requestUrlBuilder);

    } catch (HttpClientErrorException e) {
      if (Camunda7UtilRequester.isTaskNotExisting(
          httpHeaderProvider, restClient, camundaSystemUrlInfo, referencedTask.getId())) {
        return new SystemResponse(HttpStatus.OK, null);
      }
      throw e;
    }
  }

  private void setAssigneeToOwnerOfReferencedTask(
      Camunda7SystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(Camunda7SystemConnectorImpl.SET_ASSIGNEE);

    String requestBody =
        Camunda7SystemConnectorImpl.BODY_SET_ASSIGNEE + "\"" + referencedTask.getAssignee() + "\"}";

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda7RestApi();
    ResponseEntity<Void> response =
        restClient
            .post()
            .uri(requestUrlBuilder.toString())
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(requestBody)
            .retrieve()
            .toEntity(Void.class);
    LOGGER.debug(
        "Set assignee for camunda task {}. Status code = {}",
        referencedTask.getId(),
        response.getStatusCode());
  }

  private void setCompletionByKadaiAdapterAsLocalVariable(
      Camunda7SystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(Camunda7SystemConnectorImpl.LOCAL_VARIABLE_PATH)
        .append("/")
        .append(COMPLETED_BY_KADAI_ADAPTER_LOCAL_VARIABLE);

    String requestBody = "{\"value\" : true, \"type\": \"Boolean\"}";

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda7RestApi();

    ResponseEntity<Void> response =
        restClient
            .put()
            .uri(requestUrlBuilder.toString())
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(requestBody)
            .retrieve()
            .toEntity(Void.class);
    LOGGER.debug(
        "Set local Variable \"completedByKadaiAdapter\" for camunda task {}. Status code = {}",
        referencedTask.getId(),
        response.getStatusCode());
  }

  private SystemResponse performCompletion(
      Camunda7SystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask camundaTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);
    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(camundaTask.getId())
        .append(Camunda7SystemConnectorImpl.COMPLETE_TASK);

    String requestBody = prepareRequestBody(camundaTask);

    LOGGER.debug(
        "completing camunda task {}  with request body {}", camundaTask.getId(), requestBody);

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda7RestApi();

    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(requestUrlBuilder.toString())
              .headers(httpHeaders -> httpHeaders.addAll(headers))
              .body(requestBody)
              .retrieve()
              .toEntity(Void.class);
      LOGGER.debug(
          "completed camunda task {}. Status code = {}",
          camundaTask.getId(),
          response.getStatusCode());
      return new SystemResponse(response.getStatusCode(), null);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      throw new SystemException(
          "caught "
              + e.getClass().getSimpleName()
              + " "
              + e.getStatusCode()
              + " on the attempt to complete Camunda Task "
              + camundaTask.getId(),
          e.getMostSpecificCause());
    }
  }

  private String prepareRequestBody(ReferencedTask camundaTask) {

    String requestBody;
    if (camundaTask.getVariables() == null) {
      requestBody = Camunda7SystemConnectorImpl.EMPTY_REQUEST_BODY;
    } else {
      requestBody =
          Camunda7SystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES
              + camundaTask.getVariables()
              + "}}";
    }

    return requestBody;
  }
}
