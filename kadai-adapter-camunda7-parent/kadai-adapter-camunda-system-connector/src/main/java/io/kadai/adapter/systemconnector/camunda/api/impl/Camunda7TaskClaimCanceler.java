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
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Cancels claims of tasks in camunda through the camunda REST-API that have a canceled claim in
 * KADAI.
 */
@Component
public class Camunda7TaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda7TaskClaimCanceler.class);

  private final HttpHeaderProvider httpHeaderProvider;
  private final RestClient restClient;
  private final boolean claimingEnabled;
  private boolean cancelClaimConfigLogged = false;

  public Camunda7TaskClaimCanceler(
      HttpHeaderProvider httpHeaderProvider,
      Camunda7SystemConnectorConfiguration camunda7SystemConnectorConfiguration,
      RestClient restClient) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restClient = restClient;
    this.claimingEnabled = camunda7SystemConnectorConfiguration.getClaiming().isEnabled();
  }

  public SystemResponse cancelClaimOfCamunda7Task(
      Camunda7System camunda7System, ReferencedTask referencedTask) throws HttpStatusCodeException {

    if (!cancelClaimConfigLogged) {
      LOGGER.info(
          "Synchronizing CancelClaim of Tasks in KADAI to Camunda is set to {}", claimingEnabled);
      cancelClaimConfigLogged = true;
    }

    if (claimingEnabled) {

      StringBuilder requestUrlBuilder = new StringBuilder();

      requestUrlBuilder
          .append(camunda7System.getSystemRestUrl())
          .append(Camunda7SystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
          .append(referencedTask.getId())
          .append(Camunda7SystemConnectorImpl.UNCLAIM_TASK);

      String requestBody = "{}";
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
            "cancel claimed camunda task {}. Status code = {}",
            referencedTask.getId(),
            response.getStatusCode());
        return new SystemResponse(response.getStatusCode(), null);
      } catch (HttpClientErrorException e) {
        if (Camunda7UtilRequester.isTaskNotExisting(
            httpHeaderProvider, restClient, camunda7System, referencedTask.getId())) {
          return new SystemResponse(HttpStatus.OK, null);
        }
        throw e;
      }
    }

    return new SystemResponse(HttpStatus.OK, null);
  }
}
