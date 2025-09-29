package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class Camunda8TaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskClaimCanceler.class);
  private final Camunda8HttpHeaderProvider httpHeaderProvider;
  private final RestTemplate restTemplate;

  public Camunda8TaskClaimCanceler(Camunda8HttpHeaderProvider httpHeaderProvider,
                                   RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restTemplate = restTemplate;
  }

  @Value("${kadai.adapter.camunda8.claiming.enabled:true}")
  private boolean claimingEnabled;

  private boolean cancelClaimConfigLogged = false;

  public SystemResponse cancelClaimOfCamunda8Task(
          Camunda8System camunda8System,
          ReferencedTask referencedTask) {

    if (!cancelClaimConfigLogged) {
      LOGGER.info("Synchronizing CancelClaim of Tasks in KADAI to Camunda 8 is set to {}",
                claimingEnabled);
      cancelClaimConfigLogged = true;
    }

    if (claimingEnabled) {
      StringBuilder requestUrlBuilder = new StringBuilder();
      requestUrlBuilder
                .append(camunda8System.getClusterApiUrl())
                .append(Camunda8SystemConnectorImpl.URL_GET_CAMUNDA8_USER_TASKS)
                .append(referencedTask.getId())
                .append(Camunda8SystemConnectorImpl.URL_CAMUNDA8_UNCLAIM);

      String requestBody = Camunda8SystemConnectorImpl.BODY_EMPTY_REQUEST;
      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      try {
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                    requestUrlBuilder.toString(),
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class);

        LOGGER.debug(
            "Successfully canceled claim for Camunda 8 task {}. Status code = {}",
                referencedTask.getId(),
            responseEntity.getStatusCode());

        return new SystemResponse(responseEntity.getStatusCode(), null);

      } catch (HttpStatusCodeException e) {
        LOGGER.warn("Failed to cancel claim for Camunda 8 task: {}. Error: {}",
                referencedTask.getId(), e.getMessage());
        throw e;
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
