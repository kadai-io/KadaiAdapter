package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** Util class for Camunda 8 requests used in multiple components of Camunda8SystemConnectorImpl. */
public class Camunda8UtilRequester {

  private Camunda8UtilRequester() {}

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8UtilRequester.class);

  public static boolean isTaskExisting(
        Camunda8HttpHeaderProvider httpHeaderProvider,
        RestTemplate restTemplate,
        Camunda8System camunda8System,
        String userTaskKey) {

    String requestUrl = camunda8System.getClusterApiUrl()
                        + Camunda8SystemConnectorImpl.URL_GET_CAMUNDA8_USER_TASKS
                        + userTaskKey;

    HttpEntity<Void> requestEntity = new HttpEntity<>(httpHeaderProvider
            .getHttpHeadersForCamunda8TasklistApi());

    try {
      restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
    } catch (HttpStatusCodeException ex) {
      boolean isNotExisting = HttpStatus.NOT_FOUND.equals(ex.getStatusCode());
      if (isNotExisting) {
        LOGGER.debug("Camunda 8 Task {} is not existing. Returning silently", userTaskKey);
      }
      return isNotExisting;
    }
    return false;
  }

  public static String getUserTaskKeyFromReferencedTask(ReferencedTask task){
    String id = task.getId();
    return id.substring(id.lastIndexOf('-') + 1);
  }
}
