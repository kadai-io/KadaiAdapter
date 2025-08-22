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

package io.kadai.adapter.impl.scheduled;

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.util.LowerMedian;
import io.kadai.common.api.exceptions.SystemException;
import io.kadai.task.api.CallbackState;
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
import org.springframework.transaction.annotation.Transactional;

/** Cancels claims of ReferencedTasks in external system that have been cancel claimed in KADAI. */
@Component
public class ReferencedTaskClaimCanceler implements ScheduledComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskClaimCanceler.class);
  private final AdapterManager adapterManager;
  private final SchedulerRun schedulerRun;
  private final LowerMedian<Duration> runDurationLowerMedian = new LowerMedian<>(100);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds"
          + ":5000}")
  private int runIntervalMillis;

  @Autowired
  public ReferencedTaskClaimCanceler(AdapterManager adapterManager) {
    this.adapterManager = adapterManager;
    this.schedulerRun = new SchedulerRun();
  }

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks."
              + "in.milliseconds:5000}")
  @Transactional
  public void retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTasks() {
    final Instant start = Instant.now();

    synchronized (ReferencedTaskClaimCanceler.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "----retrieveCancelledClaimKadaiTasksAndCancelCorrespondingReferencedTasks started--");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTask();
              return null;
            });
        schedulerRun.touch();
      } catch (Exception ex) {
        LOGGER.debug("Caught exception while trying to cancel claim referenced tasks", ex);
      } finally {
        runDurationLowerMedian.add(Duration.between(start, Instant.now()));
      }
    }
  }

  public void retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTask() {

    try {
      KadaiConnector kadaiSystemConnector = adapterManager.getKadaiConnector();

      List<ReferencedTask> tasksCancelClaimedByKadai =
          kadaiSystemConnector.retrieveCancelledClaimKadaiTasksAsReferencedTasks();
      List<ReferencedTask> tasksCancelClaimedInExternalSystem =
          cancelClaimReferencedTasksInExternalSystem(tasksCancelClaimedByKadai);

      kadaiSystemConnector.changeTaskCallbackState(
          tasksCancelClaimedInExternalSystem, CallbackState.CALLBACK_PROCESSING_REQUIRED);
    } finally {
      LOGGER.trace(
          "ReferencedTaskClaimer."
              + "retrieveCancelledClaimKadaiTasksAndCancel"
              + "ClaimCorrespondingReferencedTask EXIT ");
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

  private List<ReferencedTask> cancelClaimReferencedTasksInExternalSystem(
      List<ReferencedTask> tasksUnclaimedByKadai) {

    List<ReferencedTask> tasksCancelClaimedInExternalSystem = new ArrayList<>();
    for (ReferencedTask referencedTaskToCancelClaim : tasksUnclaimedByKadai) {
      if (cancelClaimReferencedTask(referencedTaskToCancelClaim)) {
        tasksCancelClaimedInExternalSystem.add(referencedTaskToCancelClaim);
      }
    }
    return tasksCancelClaimedInExternalSystem;
  }

  private boolean cancelClaimReferencedTask(ReferencedTask referencedTask) {

    LOGGER.trace(
        "ENTRY to ReferencedTaskClaimer.cancelClaimReferencedTask, TaskId = {} ",
        referencedTask.getId());
    boolean success = false;
    try {
      SystemConnector connector =
          adapterManager.getSystemConnectors().get(referencedTask.getSystemUrl());
      if (connector != null) {
        connector.cancelClaimReferencedTask(referencedTask);
        success = true;
      } else {
        throw new SystemException(
            "couldnt find a connector for systemUrl " + referencedTask.getSystemUrl());
      }
    } catch (Exception ex) {
      LOGGER.error(
          "Caught {} when attempting to cancel the claim for a referenced task {}",
          ex,
          referencedTask);
    }
    LOGGER.trace(
        "Exit from ReferencedTaskClaimerCanceller.cancelClaimReferencedTask, Success = {} ",
        success);
    return success;
  }
}
