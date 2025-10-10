package io.kadai.adapter.systemconnector.camunda.api.impl;

import static io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8UtilRequester.getUserTaskKeyFromReferencedTask;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** Claims tasks in Camunda 8 through the Tasklist API that have been claimed in KADAI. */
@Component
public class Camunda8TaskClaimer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskClaimer.class);

  private final Camunda8HttpHeaderProvider httpHeaderProvider;
  private final RestTemplate restTemplate;

  public Camunda8TaskClaimer(Camunda8HttpHeaderProvider httpHeaderProvider,
                             RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restTemplate = restTemplate;
  }

  @Value("${kadai.adapter.camunda8.claiming.enabled:true}")
  private boolean claimingEnabled;

  private boolean claimConfigLogged = false;

  public SystemResponse claimCamunda8Task(
          Camunda8System camunda8System,
          ReferencedTask referencedTask) {

    if (!claimConfigLogged) {
      LOGGER.info("Synchronizing claim of tasks in KADAI to Camunda 8 is set to {}",
              claimingEnabled);
      claimConfigLogged = true;
    }

    if (claimingEnabled) {
      StringBuilder requestUrlBuilder = new StringBuilder();
      requestUrlBuilder
                .append(camunda8System.getClusterApiUrl())
                .append(Camunda8SystemConnectorImpl.URL_GET_CAMUNDA8_USER_TASKS)
                .append(getUserTaskKeyFromReferencedTask(referencedTask))
                .append(Camunda8SystemConnectorImpl.URL_CAMUNDA8_ASSIGNMENT);

      String requestBody = String.format(
                Camunda8SystemConnectorImpl.BODY_CAMUNDA8_ASSIGN,
                referencedTask.getAssignee()
      );

      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      try {
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                requestUrlBuilder.toString(),
                requestEntity,
                String.class);

        LOGGER.debug(
                    "claimed camunda 8 task {}. Status code = {}",
                    referencedTask.getId(),
                    responseEntity.getStatusCode());

        return new SystemResponse(responseEntity.getStatusCode(), null);

      } catch (HttpStatusCodeException e) {
        LOGGER.warn("Caught Exception when trying to claim camunda 8 task", e);
        throw e;
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
