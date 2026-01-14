package io.kadai.adapter.systemconnector.camunda.api.impl;

import static io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8UtilRequester.getUserTaskKeyFromReferencedTask;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Completes tasks in Camunda 8 through the Tasklist API that have been completed in KADAI. */
@Component
public class Camunda8TaskCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskCompleter.class);
  private final CamundaClient camundaClient;

  @Value("${kadai.adapter.camunda8.completing.enabled:true}")
  private boolean completingEnabled;

  private boolean completeConfigLogged = false;

  @Autowired
  public Camunda8TaskCompleter(CamundaClient camundaClient) {
    this.camundaClient = camundaClient;
  }

  public SystemResponse completeCamunda8Task(ReferencedTask referencedTask) {

    if (!completeConfigLogged) {
      LOGGER.info(
          "Synchronizing completion of tasks in KADAI to Camunda 8 is set to {}",
          completingEnabled);
      completeConfigLogged = true;
    }

    if (completingEnabled) {
      try {
        camundaClient
            .newCompleteUserTaskCommand(getUserTaskKeyFromReferencedTask(referencedTask))
            .variables(referencedTask.getVariables())
            .send()
            .join();
        return new SystemResponse(HttpStatus.NO_CONTENT, null);
      } catch (ProblemException e) {
        if (Camunda8UtilRequester.isTaskExisting(
            camundaClient, getUserTaskKeyFromReferencedTask(referencedTask))) {
          return new SystemResponse(HttpStatus.OK, null);
        } else {
          LOGGER.warn("Caught Exception when trying to complete camunda task", e);
          throw e;
        }
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
