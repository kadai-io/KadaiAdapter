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

package io.kadai.adapter.impl.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.configuration.AdapterConfiguration.SchedulerConfig;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.impl.service.KadaiTaskStarterService;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.InboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.exceptions.TaskAlreadyExistException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LambdaParameterName")
class KadaiTaskStarterOrchestratorTest {

  @Mock private AdapterManager adapterManager;
  @Mock private KadaiTaskStarterService kadaiTaskStarterService;
  @Mock private InboundSystemConnector inboundSystemConnector;

  @Test
  void should_CreateAllTasks_When_ProcessingInParallel() throws Exception {
    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(20);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(kadaiTaskStarterService, times(20)).createKadaiTask(any(ReferencedTask.class));
    verify(inboundSystemConnector).kadaiTasksHaveBeenCreatedForNewReferencedTasks(any());
  }

  @Test
  void should_UseMultipleThreads_When_ProcessingTasks() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(10);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    Set<String> threadNames = ConcurrentHashMap.newKeySet();
    CountDownLatch allStarted = new CountDownLatch(10);

    doAnswer(
            _invocation -> {
              threadNames.add(Thread.currentThread().getName());
              allStarted.countDown();
              // Small delay to ensure threads overlap
              Thread.sleep(50);
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    assertThat(threadNames)
        .as("Multiple threads should be used for parallel task creation")
        .hasSizeGreaterThan(1);
  }

  @Test
  void should_CompleteAllTasks_When_SomeTasksFail() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(5);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    AtomicInteger callCount = new AtomicInteger(0);
    doAnswer(
            _invocation -> {
              int count = callCount.getAndIncrement();
              if (count == 1 || count == 3) {
                throw new TaskCreationFailedException(
                    "task-" + count, new RuntimeException("Simulated failure"));
              }
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(kadaiTaskStarterService, times(5)).createKadaiTask(any(ReferencedTask.class));
    verify(inboundSystemConnector, times(2))
        .kadaiTaskFailedToBeCreatedForNewReferencedTask(any(ReferencedTask.class), any());
  }

  @Test
  void should_TreatAlreadyExistAsSuccess_When_TaskAlreadyExistExceptionOccurs() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(3);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    TaskAlreadyExistException alreadyExistException = mock(TaskAlreadyExistException.class);

    doAnswer(
            invocation -> {
              ReferencedTask task = invocation.getArgument(0);
              if ("task-1".equals(task.getId())) {
                throw new TaskCreationFailedException(task.getId(), alreadyExistException);
              }
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenCreatedForNewReferencedTasks(
            org.mockito.ArgumentMatchers.argThat(list -> list.size() == 3));
    verify(inboundSystemConnector, never())
        .kadaiTaskFailedToBeCreatedForNewReferencedTask(any(), any());
  }

  @Test
  void should_FetchVariables_When_VariablesAreNull() {
    setupAdapterManager();

    List<ReferencedTask> tasks = new ArrayList<>();
    ReferencedTask taskWithoutVariables = new ReferencedTask();
    taskWithoutVariables.setId("task-no-vars");
    taskWithoutVariables.setName("Task without variables");
    tasks.add(taskWithoutVariables);

    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);
    when(inboundSystemConnector.retrieveReferencedTaskVariables("task-no-vars"))
        .thenReturn("{\"fetched\":\"true\"}");

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(inboundSystemConnector).retrieveReferencedTaskVariables("task-no-vars");
    assertThat(taskWithoutVariables.getVariables()).isEqualTo("{\"fetched\":\"true\"}");
  }

  @Test
  void should_NotFetchVariables_When_VariablesAlreadyPresent() {
    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(1); // has variables set
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(inboundSystemConnector, never()).retrieveReferencedTaskVariables(anyString());
  }

  @Test
  void should_HandleEmptyTaskList_When_NoNewTasks() {
    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    setupAdapterManager();

    when(inboundSystemConnector.retrieveNewStartedReferencedTasks())
        .thenReturn(Collections.emptyList());

    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenCreatedForNewReferencedTasks(Collections.emptyList());
  }

  @Test
  void should_SetSystemUrl_For_EachTask() {
    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(3);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    for (ReferencedTask task : tasks) {
      assertThat(task.getSystemUrl()).isEqualTo("http://test.system");
    }
  }

  @Test
  void should_ProcessTasksConcurrentlyProvingParallelism() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(8);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    // Track maximum concurrency
    AtomicInteger currentConcurrency = new AtomicInteger(0);
    AtomicInteger maxConcurrency = new AtomicInteger(0);

    doAnswer(
            _invocation -> {
              int current = currentConcurrency.incrementAndGet();
              // Update max concurrency atomically
              maxConcurrency.updateAndGet(max -> Math.max(max, current));
              // Simulate work to ensure overlap
              Thread.sleep(100);
              currentConcurrency.decrementAndGet();
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    assertThat(maxConcurrency.get())
        .as(
            "Maximum concurrency should be greater than 1 to prove parallelism, " + "was: %d",
            maxConcurrency.get())
        .isGreaterThan(1);
  }

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void should_BeSignificantlyFaster_When_UsingMultipleThreads() throws Exception {
    setupAdapterManager();
    List<ReferencedTask> tasks = createReferencedTasks(20);

    // Measure single-threaded execution
    KadaiTaskStarterOrchestrator singleThreadOrchestrator = createOrchestrator(1);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    doAnswer(
            _invocation -> {
              Thread.sleep(50); // simulated per-task duration
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    Instant singleStart = Instant.now();
    singleThreadOrchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
    Duration singleThreadDuration = Duration.between(singleStart, Instant.now());

    // Reset tasks (systemUrl gets set during processing)
    tasks = createReferencedTasks(20);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    // Measure multi-threaded execution
    KadaiTaskStarterOrchestrator multiThreadOrchestrator = createOrchestrator(4);

    Instant multiStart = Instant.now();
    multiThreadOrchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
    Duration multiThreadDuration = Duration.between(multiStart, Instant.now());

    assertThat(multiThreadDuration)
        .as(
            "Multi-threaded execution (%dms) should be significantly faster than "
                + "single-threaded (%dms)",
            multiThreadDuration.toMillis(), singleThreadDuration.toMillis())
        .isLessThan(singleThreadDuration.dividedBy(2));
  }

  @Test
  void should_DefaultToOneThread_When_ThreadCountNotConfigured() {
    SchedulerConfig schedulerConfig = new SchedulerConfig();

    assertThat(schedulerConfig.getStartKadaiTasksThreadCount()).isEqualTo(1);
  }

  @Test
  void should_RespectConfiguredThreadCount() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(16);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    AtomicInteger maxConcurrency = new AtomicInteger(0);
    AtomicInteger currentConcurrency = new AtomicInteger(0);

    doAnswer(
            _invocation -> {
              int current = currentConcurrency.incrementAndGet();
              maxConcurrency.updateAndGet(max -> Math.max(max, current));
              // Wait briefly to ensure threads are busy
              Thread.sleep(80);
              currentConcurrency.decrementAndGet();
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    assertThat(maxConcurrency.get())
        .as("Concurrency should not exceed configured thread count of %d", 4)
        .isLessThanOrEqualTo(4);
    assertThat(maxConcurrency.get()).as("Should use multiple threads").isGreaterThan(1);
  }

  @Test
  void should_ReturnAllSuccessfulTasks_When_ProcessedInParallel() {
    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(10);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenCreatedForNewReferencedTasks(
            org.mockito.ArgumentMatchers.argThat(list -> list.size() == 10));
  }

  @Test
  void should_HandleMixedResultsCorrectly_When_SomeTasksFailInParallel() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(6);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    TaskAlreadyExistException alreadyExistException = mock(TaskAlreadyExistException.class);

    doAnswer(
            invocation -> {
              ReferencedTask task = invocation.getArgument(0);
              switch (task.getId()) {
                case "task-1":
                  // Already exists - should count as success
                  throw new TaskCreationFailedException(task.getId(), alreadyExistException);
                case "task-3":
                  // Real failure
                  throw new TaskCreationFailedException(
                      task.getId(), new RuntimeException("DB error"));
                case "task-5":
                  // Unexpected exception
                  throw new RuntimeException("Unexpected error");
                default:
                  // Success
                  return null;
              }
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(4);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    // Then
    // 4 successful: task-0, task-2, task-4 (direct success) + task-1 (already exists = success)
    verify(inboundSystemConnector)
        .kadaiTasksHaveBeenCreatedForNewReferencedTasks(
            org.mockito.ArgumentMatchers.argThat(list -> list.size() == 4));
    // 2 error handler calls: task-3 (creation failed) + task-5 (unexpected)
    verify(inboundSystemConnector, times(2))
        .kadaiTaskFailedToBeCreatedForNewReferencedTask(any(ReferencedTask.class), any());
    verify(inboundSystemConnector, times(2)).unlockEvent(any());
  }

  @Test
  @SuppressWarnings({
    "checkstyle:AbbreviationAsWordInName",
    "checkstyle:VariableDeclarationUsageDistance"
  })
  void should_ScaleLinearlyWithThreads_When_TasksAreIOBound() throws Exception {
    setupAdapterManager();

    doAnswer(
            _invocation -> {
              Thread.sleep(50);
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    // Measure with 1 thread
    List<ReferencedTask> tasks1 = createReferencedTasks(20);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks1);
    KadaiTaskStarterOrchestrator oneThread = createOrchestrator(1);

    Instant start1 = Instant.now();
    oneThread.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
    long duration1 = Duration.between(start1, Instant.now()).toMillis();

    // Measure with 2 threads
    List<ReferencedTask> tasks2 = createReferencedTasks(20);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks2);
    KadaiTaskStarterOrchestrator twoThreads = createOrchestrator(2);

    Instant start2 = Instant.now();
    twoThreads.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
    long duration2 = Duration.between(start2, Instant.now()).toMillis();

    // Measure with 4 threads
    List<ReferencedTask> tasks4 = createReferencedTasks(20);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks4);
    KadaiTaskStarterOrchestrator fourThreads = createOrchestrator(4);

    Instant start4 = Instant.now();
    fourThreads.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();
    long duration4 = Duration.between(start4, Instant.now()).toMillis();

    // Each doubling of threads should roughly halve the time
    assertThat(duration2)
        .as("2 threads (%dms) should be faster than 1 thread (%dms)", duration2, duration1)
        .isLessThan(duration1);

    assertThat(duration4)
        .as("4 threads (%dms) should be faster than 2 threads (%dms)", duration4, duration2)
        .isLessThan(duration2)
        .as(
            "4 threads (%dms) should be at least 2x faster than 1 thread (%dms)",
            duration4, duration1)
        .isLessThan(duration1 / 2);
  }

  @Test
  void should_RestoreInterruptAndBreak_When_InterruptedWhileWaitingForFuture() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(2);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    CountDownLatch taskStarted = new CountDownLatch(1);
    CountDownLatch interruptThread = new CountDownLatch(1);

    doAnswer(
            _invocation -> {
              taskStarted.countDown();
              // Wait to be interrupted
              interruptThread.await();
              Thread.sleep(100);
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    // Capture the main thread
    Thread testThread = Thread.currentThread();

    // Start a thread that will interrupt the main thread after task starts
    Thread interrupter =
        new Thread(
            () -> {
              try {
                taskStarted.await();
                interruptThread.countDown();
                Thread.sleep(50);
                testThread.interrupt();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });
    interrupter.setDaemon(true);
    interrupter.start();

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    assertThat(Thread.interrupted())
        .as("Interrupt flag should be set after interrupt handling")
        .isTrue();
  }

  @Test
  void should_LogAndContinue_When_ExecutionExceptionOccurs() throws Exception {
    setupAdapterManager();

    List<ReferencedTask> tasks = createReferencedTasks(3);
    when(inboundSystemConnector.retrieveNewStartedReferencedTasks()).thenReturn(tasks);

    AtomicInteger callCount = new AtomicInteger(0);
    doAnswer(
            _invocation -> {
              int count = callCount.getAndIncrement();
              if (count == 1) {
                // Simulate an exception that would be wrapped in ExecutionException
                throw new RuntimeException("Simulated exception during task creation");
              }
              return null;
            })
        .when(kadaiTaskStarterService)
        .createKadaiTask(any(ReferencedTask.class));

    KadaiTaskStarterOrchestrator orchestrator = createOrchestrator(2);
    orchestrator.retrieveReferencedTasksAndCreateCorrespondingKadaiTasks();

    verify(kadaiTaskStarterService, times(3)).createKadaiTask(any(ReferencedTask.class));
    verify(inboundSystemConnector)
        .kadaiTaskFailedToBeCreatedForNewReferencedTask(any(ReferencedTask.class), any());
  }

  private KadaiTaskStarterOrchestrator createOrchestrator(int threadCount) {
    AdapterConfiguration adapterConfiguration = new AdapterConfiguration();
    SchedulerConfig schedulerConfig = new SchedulerConfig();
    schedulerConfig.setStartKadaiTasksInterval(5000L);
    schedulerConfig.setStartKadaiTasksThreadCount(threadCount);
    adapterConfiguration.setScheduler(schedulerConfig);
    return new KadaiTaskStarterOrchestrator(
        adapterManager, adapterConfiguration, kadaiTaskStarterService);
  }

  private void setupAdapterManager() {
    when(adapterManager.getInboundSystemConnectors())
        .thenReturn(Map.of("http://test.system", inboundSystemConnector));
    when(inboundSystemConnector.getSystemUrl()).thenReturn("http://test.system");
  }

  private List<ReferencedTask> createReferencedTasks(int count) {
    List<ReferencedTask> tasks = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ReferencedTask task = new ReferencedTask();
      task.setId("task-" + i);
      task.setName("Task " + i);
      task.setVariables("{\"key\":\"value\"}");
      tasks.add(task);
    }
    return tasks;
  }
}
