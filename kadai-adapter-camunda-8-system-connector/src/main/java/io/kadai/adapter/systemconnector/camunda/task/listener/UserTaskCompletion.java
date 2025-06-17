package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.spring.client.annotation.JobWorker;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCompletion {

  private final KadaiTaskTerminator taskTerminator;
  private final ReferencedTaskCreator referencedTaskCreator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCompletion.class);

  public UserTaskCompletion(
      KadaiTaskTerminator taskTerminator, ReferencedTaskCreator referencedTaskCreator) {
    this.taskTerminator = taskTerminator;
    this.referencedTaskCreator = referencedTaskCreator;
  }

  // todo: info, trace and warning logging

  // todo: do we really need to use ActivatedJob here? By doing so, we load all variables
  @JobWorker(type = "user-task-listener-completion")
  public void receiveTaskCompletedEvent(final JobClient jobClient, final ActivatedJob job) {
    // ! mit ActivateJob: alle Variablen werden mitgeladen

    try {
      // Logic to handle task completion event
      LOGGER.info("ToDo!");

      ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);

      taskTerminator.terminateKadaiTask(referencedTask);
    } catch (Exception e) {
      LOGGER.warn(
          "caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
