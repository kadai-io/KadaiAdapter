/*
 * Copyright [2026] [envite consulting GmbH]
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
 */

package io.kadai.adapter.kadaiconnector.api.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.kadaiconnector.config.KadaiSystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.CallbackState;
import io.kadai.task.api.TaskQuery;
import io.kadai.task.api.TaskService;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KadaiSystemConnectorImplTest {

  @Mock private TaskService taskService;
  @Mock private TaskInformationMapper taskInformationMapper;
  @Mock private TaskQuery taskQuery;
  @Mock private TaskSummary taskSummary;

  private KadaiSystemConnectorImpl testSubject;

  @BeforeEach
  void setUp() {
    testSubject =
        new KadaiSystemConnectorImpl(
            taskService, taskInformationMapper, new KadaiSystemConnectorConfiguration());
  }

  @Test
  void should_ForceCompleteTask_When_ReferencedTaskIsCompleted() throws Exception {
    ReferencedTask referencedTask = arrangeExistingTask(TaskState.COMPLETED);

    testSubject.terminateKadaiTask(referencedTask);

    verify(taskService).forceCompleteTask("kadai-task-id");
    verifyCallbackProcessingCompleted();
    verify(taskService, never()).cancelTask(anyString());
    verify(taskService, never()).terminateTask(anyString());
  }

  @Test
  void should_CancelTask_When_ReferencedTaskIsCancelled() throws Exception {
    ReferencedTask referencedTask = arrangeExistingTask(TaskState.CANCELLED);

    testSubject.terminateKadaiTask(referencedTask);

    verify(taskService).cancelTask("kadai-task-id");
    verifyCallbackProcessingCompleted();
    verify(taskService, never()).forceCompleteTask(anyString());
    verify(taskService, never()).terminateTask(anyString());
  }

  @Test
  void should_TerminateTask_When_ReferencedTaskIsTerminated() throws Exception {
    ReferencedTask referencedTask = arrangeExistingTask(TaskState.TERMINATED);

    testSubject.terminateKadaiTask(referencedTask);

    verify(taskService).terminateTask("kadai-task-id");
    verifyCallbackProcessingCompleted();
    verify(taskService, never()).forceCompleteTask(anyString());
    verify(taskService, never()).cancelTask(anyString());
  }

  @Test
  void should_RejectTaskState_When_TaskStateIsNull() throws Exception {
    ReferencedTask referencedTask = arrangeExistingTask(null);

    assertThatThrownBy(() -> testSubject.terminateKadaiTask(referencedTask))
        .isInstanceOf(TaskTerminationFailedException.class);

    verifyNoTerminationOrCallbackCompletion();
  }

  @ParameterizedTest
  @EnumSource(
      value = TaskState.class,
      names = {"READY", "CLAIMED", "READY_FOR_REVIEW", "IN_REVIEW"})
  void should_RejectTaskState_When_TaskStateIsNotAnEndState(TaskState taskState) throws Exception {
    ReferencedTask referencedTask = arrangeExistingTask(taskState);

    assertThatThrownBy(() -> testSubject.terminateKadaiTask(referencedTask))
        .isInstanceOf(TaskTerminationFailedException.class);

    verifyNoTerminationOrCallbackCompletion();
  }

  private ReferencedTask arrangeExistingTask(TaskState state) {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId("external-task-id");
    referencedTask.setTaskState(state);

    when(taskService.createTaskQuery()).thenReturn(taskQuery);
    when(taskQuery.externalIdIn("external-task-id")).thenReturn(taskQuery);
    when(taskQuery.single()).thenReturn(taskSummary);
    when(taskSummary.getId()).thenReturn("kadai-task-id");

    return referencedTask;
  }

  private void verifyCallbackProcessingCompleted() {
    verify(taskService)
        .setCallbackStateForTasks(
            List.of("external-task-id"), CallbackState.CALLBACK_PROCESSING_COMPLETED);
  }

  private void verifyNoTerminationOrCallbackCompletion() throws Exception {
    verify(taskService, never()).forceCompleteTask(anyString());
    verify(taskService, never()).cancelTask(anyString());
    verify(taskService, never()).terminateTask(anyString());
    verify(taskService, never())
        .setCallbackStateForTasks(
            anyList(), eq(CallbackState.CALLBACK_PROCESSING_COMPLETED));
  }
}
