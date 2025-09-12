package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemUrls;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskCompleter.class);
  private final Camunda8HttpHeaderProvider httpHeaderProvider;
  private final RestTemplate restTemplate;



  public Camunda8TaskCompleter(Camunda8HttpHeaderProvider httpHeaderProvider,
                               RestTemplate restTemplate) {
    this.httpHeaderProvider = httpHeaderProvider;
    this.restTemplate = restTemplate;
  }

  @Value("${kadai.adapter.camunda8.completing.enabled:true}")
  private boolean completingEnabled;

  @Value("${kadai.adapter.camunda8.tasklist.url:http://localhost:8081}")
  private String tasklistBaseUrl;

  private boolean completeConfigLogged = false;

  public SystemResponse completeCamunda8Task(
      //TODO: SystemURL
      @SuppressWarnings("unused") CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask referencedTask) {

    if (!completeConfigLogged) {
      LOGGER.info("Synchronizing completion of tasks in KADAI to Camunda 8 is set to {}",
              completingEnabled);
      completeConfigLogged = true;
    }

    if (completingEnabled) {

      String userTaskKey = referencedTask.getId();

      StringBuilder requestUrlBuilder = new StringBuilder();

      requestUrlBuilder
            .append(tasklistBaseUrl) //ToDO: SystemURL
            .append(Camunda8SystemConnectorImpl.URL_GET_CAMUNDA8_USER_TASKS)
            .append(referencedTask.getId())
            .append(Camunda8SystemConnectorImpl.URL_CAMUNDA8_COMPLETION);

      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamunda8TasklistApi();
      String requestBody = prepareRequestBody(referencedTask);
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      try {
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                requestUrlBuilder.toString(),
                requestEntity,
                String.class);

        LOGGER.debug("Successfully completed Camunda 8 task {}. Status code = {}",
                userTaskKey,
                responseEntity.getStatusCode());

        return new SystemResponse(responseEntity.getStatusCode(), null);

      } catch (HttpStatusCodeException e) {
        LOGGER.warn("Failed to complete Camunda 8 task: {}. Error: {}",
                userTaskKey,
                e.getMessage());
        throw e;
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }

  private String prepareRequestBody(ReferencedTask referencedTask) {
    if (referencedTask.getVariables() == null) {
      return Camunda8SystemConnectorImpl.BODY_CAMUNDA8_COMPLETE;
    } else {
      return "{\"variables\": " + referencedTask.getVariables() + ", \"action\": \"complete\"}";
    }
  }
}
