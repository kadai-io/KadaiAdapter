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

package io.kadai.adapter.impl.scheduled;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.configuration.AdapterConfiguration.SchedulerConfig;
import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskCompletionService;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.InboundReferencedTask;
import io.kadai.adapter.systemconnector.api.InboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SimpleInboundReferencedTask;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KadaiTaskCompletionOrchestratorTest {

  @Mock private AdapterManager adapterManager;
  @Mock private KadaiTaskCompletionService kadaiTaskCompletionService;
  @Mock private InboundSystemConnector inboundSystemConnector;

  @Test
  void should_AcknowledgeOnlySuccessfulTasks_When_SomeTerminationsFail() throws Exception {
    InboundReferencedTask first = createInboundTask("first");
    InboundReferencedTask second = createInboundTask("second");
    InboundReferencedTask third = createInboundTask("third");
    InboundReferencedTask fourth = createInboundTask("fourth");
    when(inboundSystemConnector.retrieveFinishedReferencedTasks())
        .thenReturn(List.of(first, second, third, fourth));
    TaskTerminationFailedException expectedFailure =
        new TaskTerminationFailedException("second", new RuntimeException("expected"));
    RuntimeException unexpectedFailure = new RuntimeException("unexpected");
    doAnswer(
            invocation -> {
              ReferencedTask task = invocation.getArgument(0);
              if (task == second.getReferencedTask()) {
                throw expectedFailure;
              }
              if (task == third.getReferencedTask()) {
                throw unexpectedFailure;
              }
              return null;
            })
        .when(kadaiTaskCompletionService)
        .terminateKadaiTask(org.mockito.ArgumentMatchers.any());

    createOrchestrator()
        .retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(
            inboundSystemConnector);

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(
            argThat(tasks -> tasks.equals(List.of(first, fourth))));
    verify(inboundSystemConnector)
        .kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(second, expectedFailure);
    verify(inboundSystemConnector)
        .kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(third, unexpectedFailure);
  }

  @Test
  void should_AcknowledgeAllTasks_When_AllTerminationsSucceed() throws Exception {
    InboundReferencedTask first = createInboundTask("first");
    InboundReferencedTask second = createInboundTask("second");
    when(inboundSystemConnector.retrieveFinishedReferencedTasks())
        .thenReturn(List.of(first, second));

    createOrchestrator()
        .retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(
            inboundSystemConnector);

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(List.of(first, second));
    verify(inboundSystemConnector, never())
        .kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void should_AcknowledgeNoTasks_When_AllTerminationsFail() throws Exception {
    InboundReferencedTask first = createInboundTask("first");
    InboundReferencedTask second = createInboundTask("second");
    when(inboundSystemConnector.retrieveFinishedReferencedTasks())
        .thenReturn(List.of(first, second));
    TaskTerminationFailedException expectedFailure =
        new TaskTerminationFailedException("first", new RuntimeException("expected"));
    RuntimeException unexpectedFailure = new RuntimeException("unexpected");
    doThrow(expectedFailure, unexpectedFailure)
        .when(kadaiTaskCompletionService)
        .terminateKadaiTask(org.mockito.ArgumentMatchers.any());

    createOrchestrator()
        .retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(
            inboundSystemConnector);

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(List.of());
    verify(inboundSystemConnector)
        .kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(first, expectedFailure);
    verify(inboundSystemConnector)
        .kadaiTaskFailedToBeTerminatedForFinishedReferencedTask(second, unexpectedFailure);
  }

  @Test
  void should_AcknowledgeEmptyList_When_NoTasksWereRetrieved() throws Exception {
    when(inboundSystemConnector.retrieveFinishedReferencedTasks()).thenReturn(List.of());

    createOrchestrator()
        .retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks(
            inboundSystemConnector);

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(List.of());
    verify(kadaiTaskCompletionService, never())
        .terminateKadaiTask(org.mockito.ArgumentMatchers.any());
  }

  private KadaiTaskCompletionOrchestrator createOrchestrator() {
    AdapterConfiguration adapterConfiguration = new AdapterConfiguration();
    adapterConfiguration.setScheduler(new SchedulerConfig());
    return new KadaiTaskCompletionOrchestrator(
        adapterManager, adapterConfiguration, kadaiTaskCompletionService);
  }

  private InboundReferencedTask createInboundTask(String id) {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId(id);
    return new SimpleInboundReferencedTask(referencedTask);
  }
}
