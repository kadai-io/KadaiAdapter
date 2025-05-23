package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskListener {

  // It would be nicer to use an interface or so...
  private final KadaiTaskTerminator taskTerminator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskListener.class);

  public UserTaskListener(KadaiTaskTerminator taskTerminator) {
    this.taskTerminator = taskTerminator;
  }

  // todo: info, trace and warning logging

  @JobWorker(type = "user-task-listener-completion")
  public void receiveTaskCompletedEvent(final JobClient jobClient, final ActivatedJob job) {
    // ! mit ActivateJob: alle Variablen werden mitgeladen

    try {
      // todo: transform job to referenced task

      // Logic to handle task completion event
      LOGGER.info("ToDo!");

      // taskTerminator.terminateKadaiTask(null);
    } catch (Exception e) {
      LOGGER.warn(
          "caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
