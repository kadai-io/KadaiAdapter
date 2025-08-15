/*
 * Copyright [2025] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.impl;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.util.LowerMedian;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the retrieval of finished referenced tasks and termination of corresponding KADAI
 * tasks.
 */
@Component
public class KadaiTaskCompletionOrchestrator implements ScheduledComponent {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KadaiTaskCompletionOrchestrator.class);
  private final AdapterManager adapterManager;
  private final KadaiTaskCompletionService kadaiTaskCompletionService;
  private final SchedulerRun schedulerRun;
  private final LowerMedian<Duration> runDurationLowerMedian = new LowerMedian<>(100);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds"
          + ":5000}")
  private int runIntervalMillis;

  @Autowired
  public KadaiTaskCompletionOrchestrator(
      AdapterManager adapterManager, KadaiTaskCompletionService kadaiTaskCompletionService) {
    this.adapterManager = adapterManager;
    this.kadaiTaskCompletionService = kadaiTaskCompletionService;
    this.schedulerRun = new SchedulerRun();
  }

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks."
              + "in.milliseconds:5000}")
  public void retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks() {
    final Instant start = Instant.now();

    synchronized (AdapterManager.class) {
      if (!adapterManager.isInitialized()) {
        adapterManager.init();
        return;
      }
    }

    synchronized (KadaiTaskCompletionOrchestrator.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "--retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks started-----");

      try {
        for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
          UserContext.runAsUser(
              runAsUser,
              () -> {
                retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(systemConnector);
                return null;
              });
        }
        schedulerRun.touch();
      } catch (Exception e) {
        LOGGER.warn(
            "caught exception while trying to retrieve "
                + "finished referenced tasks and terminate corresponding kadai tasks",
            e);
      } finally {
        runDurationLowerMedian.add(Duration.between(start, Instant.now()));
      }
    }
  }

  public void retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(
      SystemConnector systemConnector) {
    LOGGER.trace(
        "KadaiTaskCompletionOrchestrator."
            + "retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks ENTRY ");

    try {
      List<ReferencedTask> kadaiTasksToTerminate =
          systemConnector.retrieveFinishedReferencedTasks();

      for (ReferencedTask referencedTask : kadaiTasksToTerminate) {
        try {
          kadaiTaskCompletionService.terminateKadaiTask(referencedTask);
        } catch (TaskTerminationFailedException ex) {
          LOGGER.error(
              "attempted to terminate task with external Id {} and caught exception",
              referencedTask.getId(),
              ex);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        } catch (Exception e) {
          LOGGER.warn(
              "caught unexpected Exception when attempting to terminate KadaiTask "
                  + "for referencedTask {}",
              referencedTask,
              e);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        }
      }
      systemConnector.kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(kadaiTasksToTerminate);

    } finally {
      LOGGER.trace(
          "KadaiTaskCompletionOrchestrator."
              + "retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks EXIT ");
    }
  }

  @Override
  public SchedulerRun getLastSchedulerRun() {
    return schedulerRun;
  }

  @Override
  public Duration getRunInterval() {
    return Duration.ofMillis(runIntervalMillis);
  }

  @Override
  public Duration getExpectedRunDuration() {
    return runDurationLowerMedian.get().orElse(Duration.ZERO);
  }
}
