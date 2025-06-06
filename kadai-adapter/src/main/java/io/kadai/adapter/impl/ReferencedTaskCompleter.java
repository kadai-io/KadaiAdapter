/*
 * Copyright [2024] [envite consulting GmbH]
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

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.common.api.exceptions.SystemException;
import io.kadai.task.api.CallbackState;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes ReferencedTasks in the external system after completion of corresponding KADAI tasks.
 */
@Component
public class ReferencedTaskCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskCompleter.class);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  private final AdapterManager adapterManager;
  private final LastSchedulerRun lastSchedulerRun;

  public ReferencedTaskCompleter(AdapterManager adapterManager, LastSchedulerRun lastSchedulerRun) {
    this.adapterManager = adapterManager;
    this.lastSchedulerRun = lastSchedulerRun;
  }

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.complete.referenced.tasks."
              + "in.milliseconds:5000}")
  @Transactional
  public void retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTasks() {

    synchronized (ReferencedTaskCompleter.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "--retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTasks started-------");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTask();
              return null;
            });
        lastSchedulerRun.touch();
      } catch (Exception ex) {
        LOGGER.debug("Caught exception while trying to complete referenced tasks", ex);
      }
    }
  }

  public void retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTask() {
    LOGGER.trace(
        "ReferencedTaskCompleter."
            + "retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTask ENTRY");
    try {
      KadaiConnector kadaiSystemConnector = adapterManager.getKadaiConnector();

      List<ReferencedTask> tasksCompletedByKadai =
          kadaiSystemConnector.retrieveFinishedKadaiTasksAsReferencedTasks();
      List<ReferencedTask> tasksCompletedInExternalSystem =
          completeReferencedTasksInExternalSystem(tasksCompletedByKadai);

      kadaiSystemConnector.changeTaskCallbackState(
          tasksCompletedInExternalSystem, CallbackState.CALLBACK_PROCESSING_COMPLETED);
    } finally {
      LOGGER.trace(
          "ReferencedTaskCompleter."
              + "retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTask EXIT ");
    }
  }

  public boolean completeReferencedTask(ReferencedTask referencedTask) {
    LOGGER.trace(
        "ENTRY to ReferencedTaskCompleter.completeReferencedTask, TaskId = {} ",
        referencedTask.getId());
    boolean success = false;
    try {
      SystemConnector connector =
          adapterManager.getSystemConnectors().get(referencedTask.getSystemUrl());
      if (connector != null) {
        connector.completeReferencedTask(referencedTask);
        success = true;
      } else {
        throw new SystemException(
            "couldnt find a connector for systemUrl " + referencedTask.getSystemUrl());
      }
    } catch (Exception ex) {
      LOGGER.error(
          "Caught exception when attempting to complete referenced task {}", referencedTask, ex);
    }
    LOGGER.trace(
        "Exit from ReferencedTaskCompleter.completeReferencedTask, Success = {} ", success);
    return success;
  }

  private List<ReferencedTask> completeReferencedTasksInExternalSystem(
      List<ReferencedTask> tasksCompletedByKadai) {
    List<ReferencedTask> tasksCompletedInExternalSystem = new ArrayList<>();
    for (ReferencedTask referencedTask : tasksCompletedByKadai) {
      if (completeReferencedTask(referencedTask)) {
        tasksCompletedInExternalSystem.add(referencedTask);
      }
    }
    return tasksCompletedInExternalSystem;
  }
}
