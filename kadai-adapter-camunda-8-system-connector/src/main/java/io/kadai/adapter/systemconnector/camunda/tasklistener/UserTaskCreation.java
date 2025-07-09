package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.spring.client.annotation.JobWorker;
import io.kadai.adapter.impl.KadaiTaskStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskCreation {

  private final KadaiTaskStarter taskStarter;
  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCreation.class);
  private boolean gotActivated = false;

  public UserTaskCreation(KadaiTaskStarter taskStarter) {
    this.taskStarter = taskStarter;
  }

  @JobWorker(type = "kadai-receive-task-created-event")
  // todo: this is not working for some reason, it seems like Camunda is not reaching my job worker
  // but no error is thrown
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

      // todo: handle task creation here

    } catch (Exception e) {
      LOGGER.error(
          "Caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
