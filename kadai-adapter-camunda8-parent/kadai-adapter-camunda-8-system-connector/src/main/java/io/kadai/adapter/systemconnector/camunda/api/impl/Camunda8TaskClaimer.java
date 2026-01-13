package io.kadai.adapter.systemconnector.camunda.api.impl;

import static io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8UtilRequester.getUserTaskKeyFromReferencedTask;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Claims tasks in Camunda 8 through the Tasklist API that have been claimed in KADAI. */
@Component
public class Camunda8TaskClaimer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskClaimer.class);

  private final CamundaClient camundaClient;

  @Value("${kadai.adapter.camunda8.claiming.enabled:true}")
  private boolean claimingEnabled;

  private boolean claimConfigLogged = false;

  public Camunda8TaskClaimer(@Autowired CamundaClient camundaClient) {
    this.camundaClient = camundaClient;
  }

  public SystemResponse claimCamunda8Task(ReferencedTask referencedTask) {

    if (!claimConfigLogged) {
      LOGGER.info(
          "Synchronizing claim of tasks in KADAI to Camunda 8 is set to {}", claimingEnabled);
      claimConfigLogged = true;
    }

    if (claimingEnabled) {
      try {
        camundaClient
            .newAssignUserTaskCommand(getUserTaskKeyFromReferencedTask(referencedTask))
            .assignee(referencedTask.getAssignee())
            .send()
            .join();
        return new SystemResponse(HttpStatus.NO_CONTENT, null);
      } catch (Exception e) {
        LOGGER.warn("Caught Exception when trying to claim camunda 8 task", e);
        throw e;
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
