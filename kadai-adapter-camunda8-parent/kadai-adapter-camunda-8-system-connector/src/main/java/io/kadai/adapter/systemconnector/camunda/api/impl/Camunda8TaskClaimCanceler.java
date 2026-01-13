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

@Component
public class Camunda8TaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda8TaskClaimCanceler.class);
  private final CamundaClient camundaClient;

  @Value("${kadai.adapter.camunda8.claiming.enabled:true}")
  private boolean claimingEnabled;

  private boolean cancelClaimConfigLogged = false;

  public Camunda8TaskClaimCanceler(@Autowired CamundaClient camundaClient) {
    this.camundaClient = camundaClient;
  }

  public SystemResponse cancelClaimOfCamunda8Task(ReferencedTask referencedTask) {
    if (!cancelClaimConfigLogged) {
      LOGGER.info(
          "Synchronizing CancelClaim of Tasks in KADAI to Camunda 8 is set to {}", claimingEnabled);
      cancelClaimConfigLogged = true;
    }

    if (claimingEnabled) {
      try {
        camundaClient
            .newUnassignUserTaskCommand(getUserTaskKeyFromReferencedTask(referencedTask))
            .send()
            .join();
        return new SystemResponse(HttpStatus.NO_CONTENT, null);
      } catch (Exception e) {
        LOGGER.warn(
            "Failed to cancel claim for Camunda 8 task: {}. Error: {}",
            referencedTask.getId(),
            e.getMessage());
        throw e;
      }
    }
    return new SystemResponse(HttpStatus.OK, null);
  }
}
