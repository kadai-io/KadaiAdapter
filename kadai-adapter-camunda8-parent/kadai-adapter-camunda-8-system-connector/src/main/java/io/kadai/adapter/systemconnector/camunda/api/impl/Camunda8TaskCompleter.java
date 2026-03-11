package io.kadai.adapter.systemconnector.camunda.api.impl;

import static io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator.extractUserTaskKeyFromTaskId;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8SystemConnectorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Completes tasks in Camunda 8 through the Tasklist API that have been completed in KADAI. */
@Component
public class Camunda8TaskCompleter {

  public static final String USER_TASK_COMPLETED_BY_KADAI_ACTION = "completed-by-kadai";
  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskCompleter.class);

  private final CamundaClient camundaClient;
  private final boolean completingEnabled;
  private boolean completeConfigLogged = false;

  @Autowired
  public Camunda8TaskCompleter(
      CamundaClient camundaClient, Camunda8SystemConnectorConfiguration connectorConfiguration) {
    this.camundaClient = camundaClient;
    this.completingEnabled = connectorConfiguration.getCompleting().isEnabled();
  }

  public SystemResponse completeCamunda8Task(ReferencedTask referencedTask) {

    if (!completeConfigLogged) {
      LOGGER.info(
          "Synchronizing completion of tasks in KADAI to Camunda 8 is set to {}",
          completingEnabled);
      completeConfigLogged = true;
    }

    if (completingEnabled) {
      final Long userTaskKey = extractUserTaskKeyFromTaskId(referencedTask.getId());
      try {
        camundaClient
            .newCompleteUserTaskCommand(userTaskKey)
            .variables(String.format("{%s}", referencedTask.getVariables()))
            .action(USER_TASK_COMPLETED_BY_KADAI_ACTION)
            .send()
            .join();
        return new SystemResponse(HttpStatus.NO_CONTENT, null);
      } catch (ClientException e) {
        if (Camunda8UtilRequester.isTaskExisting(camundaClient, userTaskKey)) {
          LOGGER.warn(
              "Completion of Task {} encountered problems: {}",
              referencedTask.getId(),
              e.getMessage());
          return new SystemResponse(HttpStatus.OK, null);
        } else {
          LOGGER.warn("Caught Exception when trying to complete Camunda-Task", e);
          throw e;
        }
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
