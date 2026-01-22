package io.kadai.adapter.systemconnector.camunda.tasklistener;

import static io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8TaskCompleter.USER_TASK_COMPLETED_BY_KADAI_ACTION;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskCompletionService;
import io.kadai.adapter.monitoring.MonitoredComponent;
import io.kadai.adapter.monitoring.MonitoredRun;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
import io.kadai.adapter.util.LowerMedian;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCancellation implements MonitoredComponent {

  public static final String USER_TASK_CANCELLED_JOB_WORKER_TYPE =
      "kadai-receive-task-cancelled-event";
  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCancellation.class);
  private static final String TASK_STATE_CANCELLED = "CANCELLED";

  private final KadaiTaskCompletionService taskTerminator;
  private final ReferencedTaskCreator referencedTaskCreator;
  private final MonitoredRun monitoredRun = new MonitoredRun();
  private final LowerMedian<Duration> runDurationLowerMedian = new LowerMedian<>(100);

  public UserTaskCancellation(
      KadaiTaskCompletionService taskTerminator, ReferencedTaskCreator referencedTaskCreator) {
    this.taskTerminator = taskTerminator;
    this.referencedTaskCreator = referencedTaskCreator;
  }

  @JobWorker(type = USER_TASK_CANCELLED_JOB_WORKER_TYPE)
  public void receiveTaskCancelledEvent(final ActivatedJob job)
      throws TaskTerminationFailedException {
    monitoredRun.start();
    LOGGER.info(
        "UserTaskListener kadai-receive-task-cancelled-event has been called, "
            + "connected to process instance '{}'",
        job.getProcessInstanceKey());

    final String userTaskAction = Objects.toString(job.getUserTask().getAction(), "null");
    if (userTaskAction.equals(USER_TASK_COMPLETED_BY_KADAI_ACTION)) {
      LOGGER.debug("Cancellation was initiated by Kadai. Skipping cancel to avoid circle.");
      monitoredRun.succeed();
      return;
    }

    try {
      ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);
      referencedTask.setTaskState(TASK_STATE_CANCELLED);
      taskTerminator.terminateKadaiTask(referencedTask);
      monitoredRun.succeed();
    } catch (Exception e) {
      monitoredRun.fail();
      throw e;
    } finally {
      runDurationLowerMedian.add(monitoredRun.getDuration());
    }
  }

  @Override
  public MonitoredRun getLastRun() {
    return monitoredRun;
  }

  @Override
  public Duration getExpectedRunDuration() {
    return runDurationLowerMedian.get().orElse(Duration.ZERO);
  }
}
