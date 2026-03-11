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

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.impl.util.UserContext;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.models.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KadaiTaskStarterServiceTest {

  @Mock private AdapterManager adapterManager;
  @Mock private AdapterConfiguration adapterConfiguration;
  @Mock private KadaiConnector kadaiConnector;

  @Mock private Task kadaiTask;

  private KadaiTaskStarterService testSubject;
  private ReferencedTask referencedTask;

  @BeforeEach
  void setUp() {
    when(adapterConfiguration.getRunAsUser()).thenReturn("test-run-as-user");
    testSubject = new KadaiTaskStarterServiceImpl(adapterManager, adapterConfiguration);

    // Create a sample ReferencedTask for testing
    referencedTask = new ReferencedTask();
    referencedTask.setId("test-task-id");
    referencedTask.setName("Test Task");
    referencedTask.setSystemUrl("http://test.system");
    referencedTask.setTaskDefinitionKey("testTask");
  }

  @Test
  void should_CreateKadaiTask_When_ValidReferencedTaskProvided()
      throws TaskCreationFailedException {
    // Given
    when(adapterManager.getKadaiConnector()).thenReturn(kadaiConnector);
    when(kadaiConnector.convertToKadaiTask(referencedTask)).thenReturn(kadaiTask);

    // When
    testSubject.createKadaiTask(referencedTask);

    // Then
    verify(adapterManager).getKadaiConnector();
    verify(kadaiConnector).convertToKadaiTask(referencedTask);
    verify(kadaiConnector).createKadaiTask(kadaiTask);
    verifyNoMoreInteractions(adapterManager, kadaiConnector, kadaiTask);
  }

  @Test
  void should_UseRunAsUser_When_CreatingTask() throws TaskCreationFailedException {
    // Given
    MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);

    // When
    testSubject.createKadaiTask(referencedTask);

    // Then
    userContextMock.verify(() -> UserContext.runAsUser(eq("test-run-as-user"), any()));
    userContextMock.close();
  }

  @Test
  void should_ThrowTaskCreationFailedException_When_CreateKadaiTaskFails()
      throws TaskCreationFailedException {
    // Given
    when(adapterManager.getKadaiConnector()).thenReturn(kadaiConnector);
    when(kadaiConnector.convertToKadaiTask(referencedTask)).thenReturn(kadaiTask);
    doThrow(
            new TaskCreationFailedException(
                "test-task-id", new RuntimeException("Task creation failed")))
        .when(kadaiConnector)
        .createKadaiTask(kadaiTask);

    // When & Then
    assertThatThrownBy(() -> testSubject.createKadaiTask(referencedTask))
        .isInstanceOf(TaskCreationFailedException.class);

    verify(adapterManager).getKadaiConnector();
    verify(kadaiConnector).convertToKadaiTask(referencedTask);
    verify(kadaiConnector).createKadaiTask(kadaiTask);
    verifyNoMoreInteractions(adapterManager, kadaiConnector, kadaiTask);
  }
}
