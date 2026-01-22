package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskStarterService;
import io.kadai.adapter.monitoring.MonitoredComponent;
import io.kadai.adapter.monitoring.MonitoredRun;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.tasklistener.util.ReferencedTaskCreator;
import io.kadai.adapter.util.LowerMedian;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserTaskCreation implements MonitoredComponent {

  public static final String USER_TASK_CREATED_JOB_WORKER_TYPE = "kadai-receive-task-created-event";
  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskCreation.class);

  private final KadaiTaskStarterService taskStarter;
  private final ReferencedTaskCreator referencedTaskCreator;
  private final MonitoredRun monitoredRun = new MonitoredRun();
  private final LowerMedian<Duration> runDurationLowerMedian = new LowerMedian<>(100);

  public UserTaskCreation(
      KadaiTaskStarterService taskStarter, ReferencedTaskCreator referencedTaskCreator) {
    this.taskStarter = taskStarter;
    this.referencedTaskCreator = referencedTaskCreator;
  }

  @JobWorker(type = USER_TASK_CREATED_JOB_WORKER_TYPE)
  public void receiveTaskCreatedEvent(final ActivatedJob job) throws TaskCreationFailedException {
    monitoredRun.start();
    LOGGER.info(
        "UserTaskListener kadai-receive-task-created-event has been called, "
            + "connected to process instance '{}'",
        job.getProcessInstanceKey());
    try {
      ReferencedTask referencedTask = referencedTaskCreator.createReferencedTaskFromJob(job);
      taskStarter.createKadaiTask(referencedTask);
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
