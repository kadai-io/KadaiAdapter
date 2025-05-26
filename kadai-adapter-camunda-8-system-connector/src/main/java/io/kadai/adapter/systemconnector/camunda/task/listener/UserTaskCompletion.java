package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.kadai.adapter.impl.KadaiTaskTerminator;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCompletion {

  private final KadaiTaskTerminator taskTerminator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCompletion.class);

  public UserTaskCompletion(KadaiTaskTerminator taskTerminator) {
    this.taskTerminator = taskTerminator;
  }

  // todo: info, trace and warning logging

  @JobWorker(type = "user-task-listener-completion")
  public void receiveTaskCompletedEvent(final JobClient jobClient, final ActivatedJob job) {
    // ! mit ActivateJob: alle Variablen werden mitgeladen

    try {
      // Logic to handle task completion event
      LOGGER.info("ToDo!");

      // todo: implement logic to transform job to referenced task, using custom headers

      // taskTerminator.terminateKadaiTask(null);
    } catch (Exception e) {
      LOGGER.warn(
          "caught exception while trying to retrieve "
              + "finished referenced tasks and terminate corresponding kadai tasks",
          e);
    }
  }
}
