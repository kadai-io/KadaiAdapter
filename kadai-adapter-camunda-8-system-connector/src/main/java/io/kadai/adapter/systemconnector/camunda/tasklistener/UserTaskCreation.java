package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.spring.client.annotation.JobWorker;
import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8SystemConnectorImpl;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCreation {

  private final KadaiTaskStarter taskStarter;
  private final ReferencedTaskCreator referencedTaskCreator;
  private final Camunda8SystemConnectorImpl
      systemConnector; // todo: it is not really nice to have this here, but it is needed

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCreation.class);
  private boolean gotActivated = false;

  public UserTaskCreation(
      KadaiTaskStarter taskStarter,
      ReferencedTaskCreator referencedTaskCreator,
      Camunda8SystemConnectorImpl systemConnector) {
    this.taskStarter = taskStarter;
    this.referencedTaskCreator = referencedTaskCreator;
    this.systemConnector = systemConnector;
  }

  @JobWorker(type = "kadai-receive-task-created-event")
  public void receiveTaskCreatedEvent(final ActivatedJob job) {

    try {
      if (!gotActivated) {
        gotActivated = true;
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              "UserTaskListener kadai-receive-task-created-event activated successfully, "
                  + "connected to process instance {}",
              job.getProcessInstanceKey());
        }

        ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);
        taskStarter.createAndStartKadaiTasks(
            systemConnector, new ArrayList<>(List.of(referencedTask)));
      }

    } catch (Exception e) {
      LOGGER.error(
          "Caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
