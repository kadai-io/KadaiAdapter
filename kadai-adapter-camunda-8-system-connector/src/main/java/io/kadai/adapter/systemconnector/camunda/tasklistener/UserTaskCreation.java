package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.spring.client.annotation.JobWorker;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskStarterService;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCreation {

  private final KadaiTaskStarterService taskStarter;
  private final ReferencedTaskCreator referencedTaskCreator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCreation.class);

  public UserTaskCreation(
      KadaiTaskStarterService taskStarter, ReferencedTaskCreator referencedTaskCreator) {
    this.taskStarter = taskStarter;
    this.referencedTaskCreator = referencedTaskCreator;
  }

  @JobWorker(type = "kadai-receive-task-created-event")
  public void receiveTaskCreatedEvent(final ActivatedJob job) {

    try {
      LOGGER.info(
          "UserTaskListener kadai-receive-task-created-event has been called, "
              + "connected to process instance '{}'",
          job.getProcessInstanceKey());

      ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);

      taskStarter.createKadaiTask(referencedTask);

    } catch (TaskCreationFailedException e) {
      LOGGER.error(
          "Caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
