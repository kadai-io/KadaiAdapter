package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.spring.client.annotation.JobWorker;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.impl.scheduled.UserContext;
import io.kadai.adapter.impl.service.KadaiTaskStarterService;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCreation {

  private final KadaiTaskStarterService taskStarter;
  private final ReferencedTaskCreator referencedTaskCreator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCreation.class);
  private boolean gotActivated = false;

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  public UserTaskCreation(
      KadaiTaskStarterService taskStarter,
      ReferencedTaskCreator referencedTaskCreator) {
    this.taskStarter = taskStarter;
    this.referencedTaskCreator = referencedTaskCreator;
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
      }

      ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);

      UserContext.runAsUser(
          runAsUser,
          () -> {
            try {
              taskStarter.createKadaiTask(referencedTask);
            } catch (TaskCreationFailedException e) {
              throw new RuntimeException(e);
            }
            return null;
          });

    } catch (Exception e) {
      LOGGER.error(
          "Caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
