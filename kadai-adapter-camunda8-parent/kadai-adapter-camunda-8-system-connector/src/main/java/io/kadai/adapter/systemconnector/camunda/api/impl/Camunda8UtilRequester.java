package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Util class for Camunda 8 requests used in multiple components of Camunda8SystemConnectorImpl. */
public class Camunda8UtilRequester {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8UtilRequester.class);

  private Camunda8UtilRequester() {}

  public static boolean isTaskExisting(CamundaClient camundaClient, Long userTaskKey) {
    try {
      camundaClient.newUserTaskGetRequest(userTaskKey).send().join();
      return true;
    } catch (ClientException e) {
      LOGGER.debug("Camunda 8 Task {} was not found. Returning silently", userTaskKey);
      return false;
    }
  }

  public static Long getUserTaskKeyFromReferencedTask(ReferencedTask task) {
    String id = task.getId();
    return Long.parseLong(id.substring(id.lastIndexOf('-') + 1));
  }
}
