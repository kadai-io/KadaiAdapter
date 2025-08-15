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

import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.util.LowerMedian;
import io.kadai.task.api.exceptions.TaskAlreadyExistException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Orchestrates the retrieval of new referenced tasks and creation of corresponding KADAI tasks. */
@Component
public class KadaiTaskStarterOrchestrator implements ScheduledComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiTaskStarterOrchestrator.class);
  private final AdapterManager adapterManager;
  private final KadaiTaskStarterService kadaiTaskStarterService;
  private final SchedulerRun schedulerRun;
  private final LowerMedian<Duration> runDurationLowerMedian = new LowerMedian<>(100);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Value("${kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds:5000}")
  private int runIntervalMillis;

  @Autowired
  public KadaiTaskStarterOrchestrator(
      AdapterManager adapterManager, KadaiTaskStarterService kadaiTaskStarterService) {
    this.adapterManager = adapterManager;
    this.kadaiTaskStarterService = kadaiTaskStarterService;
    this.schedulerRun = new SchedulerRun();
  }

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds:5000}")
  public void retrieveNewReferencedTasksAndCreateCorrespondingKadaiTasks() {
    final Instant start = Instant.now();
    if (!adapterIsInitialized()) {
      return;
    }
    synchronized (KadaiTaskStarterOrchestrator.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "-retrieveNewReferencedTasksAndCreateCorrespondingKadaiTasks started---------------");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
              return null;
            });
        schedulerRun.touch();
      } catch (Exception ex) {
        LOGGER.error(
            "Caught exception while trying to create Kadai tasks from referenced tasks", ex);
      } finally {
        runDurationLowerMedian.add(Duration.between(start, Instant.now()));
      }
    }
  }

  public void retrieveReferencedTasksAndCreateCorrespondingKadaiTasks() {
    LOGGER.trace(
        "KadaiTaskStarterOrchestrator."
            + "retrieveReferencedTasksAndCreateCorrespondingKadaiTasks ENTRY");
    for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
      try {
        List<ReferencedTask> tasksToStart = systemConnector.retrieveNewStartedReferencedTasks();

        List<ReferencedTask> newCreatedTasksInKadai =
            createAndStartKadaiTasks(systemConnector, tasksToStart);

        systemConnector.kadaiTasksHaveBeenCreatedForNewReferencedTasks(newCreatedTasksInKadai);
      } finally {
        LOGGER.trace(
            "KadaiTaskStarterOrchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks "
                + "Leaving handling of new tasks for System Connector {}",
            systemConnector.getSystemUrl());
      }
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

  private List<ReferencedTask> createAndStartKadaiTasks(
      SystemConnector systemConnector, List<ReferencedTask> tasksToStart) {
    List<ReferencedTask> newCreatedTasksInKadai = new ArrayList<>();
    for (ReferencedTask referencedTask : tasksToStart) {
      try {
        addVariablesToReferencedTask(referencedTask, systemConnector);
        referencedTask.setSystemUrl(systemConnector.getSystemUrl());
        kadaiTaskStarterService.createKadaiTask(referencedTask);
        newCreatedTasksInKadai.add(referencedTask);
      } catch (TaskCreationFailedException e) {
        if (e.getCause() instanceof TaskAlreadyExistException) {
          newCreatedTasksInKadai.add(referencedTask);
        } else {
          handleError(
              systemConnector,
              referencedTask,
              e,
              "caught Exception when attempting to start KadaiTask for referencedTask {}");
        }
      } catch (Exception e) {
        handleError(
            systemConnector,
            referencedTask,
            e,
            "caught unexpected Exception when attempting to start KadaiTask "
                + "for referencedTask {}");
      }
    }
    return newCreatedTasksInKadai;
  }

  private void handleError(
      SystemConnector systemConnector,
      ReferencedTask referencedTask,
      Exception exception,
      String message) {
    LOGGER.warn(message, referencedTask, exception);
    systemConnector.kadaiTaskFailedToBeCreatedForNewReferencedTask(referencedTask, exception);
    systemConnector.unlockEvent(referencedTask.getOutboxEventId());
  }

  private void addVariablesToReferencedTask(
      ReferencedTask referencedTask, SystemConnector connector) {
    if (referencedTask.getVariables() == null) {
      String variables = connector.retrieveReferencedTaskVariables(referencedTask.getId());
      referencedTask.setVariables(variables);
    }
  }

  private boolean adapterIsInitialized() {
    synchronized (AdapterManager.class) {
      if (!adapterManager.isInitialized()) {
        adapterManager.init();
        return false;
      }
      return true;
    }
  }
}
