package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskCompletionService;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCancellation {

  private final KadaiTaskCompletionService taskTerminator;
  private final ReferencedTaskCreator referencedTaskCreator;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCancellation.class);
  private static final String TASK_STATE_CANCELLED = "CANCELLED";

  public UserTaskCancellation(KadaiTaskCompletionService taskTerminator,
      ReferencedTaskCreator referencedTaskCreator) {
    this.taskTerminator = taskTerminator;
    this.referencedTaskCreator = referencedTaskCreator;
  }

  @JobWorker(type = "kadai-receive-task-cancelled-event")
  public void receiveTaskCompletedEvent(final ActivatedJob job)
      throws TaskTerminationFailedException {
    LOGGER.info(
        "UserTaskListener kadai-receive-task-cancelled-event has been called, "
            + "connected to process instance '{}'",
        job.getProcessInstanceKey());

    ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);
    referencedTask.setTaskState(TASK_STATE_CANCELLED);
    taskTerminator.terminateKadaiTask(referencedTask);
  }
}
