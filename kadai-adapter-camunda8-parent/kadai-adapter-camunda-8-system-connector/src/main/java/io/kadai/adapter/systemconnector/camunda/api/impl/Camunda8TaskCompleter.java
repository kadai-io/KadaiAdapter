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

/** Completes tasks in Camunda 8 through the Tasklist API that have been completed in KADAI. */
@Component
public class Camunda8TaskCompleter {

  public static final String TASK_COMPLETED_BY_KADAI_KV_PAIR = "\"completedByKadai\":true";
  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskCompleter.class);
  private final Camunda8HttpHeaderProvider httpHeaderProvider;
  private final RestTemplate restTemplate;

  @Value("${kadai.adapter.camunda8.completing.enabled:true}")
  private boolean completingEnabled;

  private boolean completeConfigLogged = false;

  public Camunda8TaskCompleter(
      Camunda8HttpHeaderProvider httpHeaderProvider, RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restTemplate = restTemplate;
  }

  public SystemResponse completeCamunda8Task(
      Camunda8System camunda8System, ReferencedTask referencedTask) {

    if (!completeConfigLogged) {
      LOGGER.info(
          "Synchronizing completion of tasks in KADAI to Camunda 8 is set to {}",
          completingEnabled);
      completeConfigLogged = true;
    }

    if (completingEnabled) {
      StringBuilder requestUrlBuilder = new StringBuilder();
      requestUrlBuilder
          .append(camunda8System.getRestAddress())
          .append(Camunda8SystemConnectorImpl.URL_GET_CAMUNDA8_USER_TASKS)
          .append(getUserTaskKeyFromReferencedTask(referencedTask))
          .append(Camunda8SystemConnectorImpl.URL_CAMUNDA8_COMPLETION);

      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
      String requestBody = prepareRequestBody(referencedTask);
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      try {
        ResponseEntity<String> responseEntity =
            restTemplate.postForEntity(requestUrlBuilder.toString(), requestEntity, String.class);

        LOGGER.debug(
            "Successfully completed Camunda 8 task {}. Status code = {}",
            referencedTask.getId(),
            responseEntity.getStatusCode());

        return new SystemResponse(responseEntity.getStatusCode(), null);

      } catch (HttpStatusCodeException e) {
        if (Camunda8UtilRequester.isTaskExisting(
            httpHeaderProvider,
            restTemplate,
            camunda8System,
            getUserTaskKeyFromReferencedTask(referencedTask))) {
          return new SystemResponse(HttpStatus.OK, null);
        } else {
          LOGGER.warn("Caught Exception when trying to complete camunda task", e);
          throw e;
        }
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }

  private String prepareRequestBody(ReferencedTask camundaTask) {

    String requestBody;
    if (camundaTask.getVariables() == null) {
      requestBody = Camunda8SystemConnectorImpl.BODY_EMPTY_REQUEST;
    } else {
      requestBody =
          String.format(
              Camunda8SystemConnectorImpl.BODY_CAMUNDA8_COMPLETE, TASK_COMPLETED_BY_KADAI_KV_PAIR);
    }
    return requestBody;
  }
}
