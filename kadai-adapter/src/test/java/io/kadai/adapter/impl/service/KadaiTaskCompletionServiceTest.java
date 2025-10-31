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

package io.kadai.adapter.impl.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.impl.util.UserContext;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KadaiTaskCompletionServiceTest {

  @Mock private AdapterManager adapterManager;

  @Mock private KadaiConnector kadaiConnector;

  private ReferencedTask referencedTask;
  private KadaiTaskCompletionService testSubject;

  @BeforeEach
  void setUp() {
    testSubject = new KadaiTaskCompletionServiceImpl(adapterManager, "test-run-as-user");

    // Create a sample ReferencedTask for testing
    referencedTask = new ReferencedTask();
    referencedTask.setId("test-task-id");
    referencedTask.setName("Test Task");
    referencedTask.setSystemUrl("http://test.system");
    referencedTask.setTaskDefinitionKey("testTask");
    referencedTask.setAssignee("testUser");
  }

  @Test
  void should_TerminateKadaiTask_When_ValidReferencedTaskProvided()
      throws TaskTerminationFailedException {
    // Given
    when(adapterManager.getKadaiConnector()).thenReturn(kadaiConnector);

    // When
    testSubject.terminateKadaiTask(referencedTask);

    // Then
    verify(adapterManager).getKadaiConnector();
    verify(kadaiConnector).terminateKadaiTask(referencedTask);
    verifyNoMoreInteractions(adapterManager, kadaiConnector);
  }

  @Test
  void should_UseRunAsUser_When_TerminatingTask() throws TaskTerminationFailedException {
    // Given
    MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);

    // When
    testSubject.terminateKadaiTask(referencedTask);

    // Then
    userContextMock.verify(() -> UserContext.runAsUser(eq("test-run-as-user"), any()));
    userContextMock.close();
  }

  @Test
  void should_ThrowTaskTerminationFailedException_When_TerminateKadaiTaskFails()
      throws TaskTerminationFailedException {
    // Given
    when(adapterManager.getKadaiConnector()).thenReturn(kadaiConnector);
    doThrow(
            new TaskTerminationFailedException(
                "test-task-id", new RuntimeException("Task creation failed")))
        .when(kadaiConnector)
        .terminateKadaiTask(referencedTask);

    // When & Then
    assertThatThrownBy(() -> testSubject.terminateKadaiTask(referencedTask))
        .isInstanceOf(TaskTerminationFailedException.class);

    verify(adapterManager).getKadaiConnector();
    verify(kadaiConnector).terminateKadaiTask(referencedTask);
    verifyNoMoreInteractions(adapterManager, kadaiConnector);
  }
}
