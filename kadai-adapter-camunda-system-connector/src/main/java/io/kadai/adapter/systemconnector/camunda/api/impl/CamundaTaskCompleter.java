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
import io.kadai.adapter.systemconnector.camunda.config.Camunda7Systems.Camunda7System;
import io.kadai.common.api.exceptions.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** Completes Camunda Tasks via the Camunda REST Api. */
public class CamundaTaskCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskCompleter.class);

  private static final String COMPLETED_BY_KADAI_ADAPTER_LOCAL_VARIABLE = "completedByKadaiAdapter";
  private final HttpHeaderProvider httpHeaderProvider;
  private final RestTemplate restTemplate;

  public CamundaTaskCompleter(HttpHeaderProvider httpHeaderProvider, RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restTemplate = restTemplate;
  }

  public SystemResponse completeCamundaTask(
      Camunda7System camunda7System, ReferencedTask referencedTask) {

    StringBuilder requestUrlBuilder = new StringBuilder();
    try {
      setAssigneeToOwnerOfReferencedTask(camunda7System, referencedTask, requestUrlBuilder);
      setCompletionByKadaiAdapterAsLocalVariable(camunda7System, referencedTask, requestUrlBuilder);

      return performCompletion(camunda7System, referencedTask, requestUrlBuilder);

    } catch (HttpStatusCodeException e) {
      if (CamundaUtilRequester.isTaskNotExisting(
          httpHeaderProvider, restTemplate, camunda7System, referencedTask.getId())) {
        return new SystemResponse(HttpStatus.OK, null);
      } else {
        LOGGER.warn("Caught Exception when trying to complete camunda task", e);
        throw e;
      }
    }
  }

  private void setAssigneeToOwnerOfReferencedTask(
      Camunda7System camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(CamundaSystemConnectorImpl.SET_ASSIGNEE);

    String requestBody =
        CamundaSystemConnectorImpl.BODY_SET_ASSIGNEE + "\"" + referencedTask.getAssignee() + "\"}";

    HttpEntity<String> requestEntity =
        httpHeaderProvider.prepareNewEntityForCamundaRestApi(requestBody);
    ResponseEntity<String> responseEntity =
        this.restTemplate.exchange(
            requestUrlBuilder.toString(), HttpMethod.POST, requestEntity, String.class);
    LOGGER.debug(
        "Set assignee for camunda task {}. Status code = {}",
        referencedTask.getId(),
        responseEntity.getStatusCode());
  }

  private void setCompletionByKadaiAdapterAsLocalVariable(
      Camunda7System camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(CamundaSystemConnectorImpl.LOCAL_VARIABLE_PATH)
        .append("/")
        .append(COMPLETED_BY_KADAI_ADAPTER_LOCAL_VARIABLE);

    HttpEntity<String> requestEntity =
        httpHeaderProvider.prepareNewEntityForCamundaRestApi(
            "{\"value\" : true, \"type\": \"Boolean\"}");

    ResponseEntity<String> responseEntity =
        this.restTemplate.exchange(
            requestUrlBuilder.toString(), HttpMethod.PUT, requestEntity, String.class);
    LOGGER.debug(
        "Set local Variable \"completedByKadaiAdapter\" for camunda task {}. Status code = {}",
        referencedTask.getId(),
        responseEntity.getStatusCode());
  }

  private SystemResponse performCompletion(
      Camunda7System camundaSystemUrlInfo,
      ReferencedTask camundaTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);
    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(camundaTask.getId())
        .append(CamundaSystemConnectorImpl.COMPLETE_TASK);

    String requestBody = prepareRequestBody(camundaTask);

    LOGGER.debug(
        "completing camunda task {}  with request body {}", camundaTask.getId(), requestBody);

    HttpEntity<String> entity = httpHeaderProvider.prepareNewEntityForCamundaRestApi(requestBody);

    try {
      ResponseEntity<String> responseEntity =
          restTemplate.postForEntity(requestUrlBuilder.toString(), entity, String.class);
      LOGGER.debug(
          "completed camunda task {}. Status code = {}",
          camundaTask.getId(),
          responseEntity.getStatusCode());

      return new SystemResponse(responseEntity.getStatusCode(), null);

    } catch (HttpStatusCodeException e) {
      LOGGER.info(
          "tried to complete camunda task {} and caught Status code {}",
          camundaTask.getId(),
          e.getStatusCode());
      throw new SystemException(
          "caught HttpStatusCodeException "
              + e.getStatusCode()
              + " on the attempt to complete Camunda Task "
              + camundaTask.getId(),
          e.getMostSpecificCause());
    }
  }

  private String prepareRequestBody(ReferencedTask camundaTask) {

    String requestBody;
    if (camundaTask.getVariables() == null) {
      requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
    } else {
      requestBody =
          CamundaSystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES + camundaTask.getVariables() + "}}";
    }

    return requestBody;
  }
}
