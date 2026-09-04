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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.kadaiconnector.config.KadaiSystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.TaskService;
import io.kadai.task.api.models.ObjectReference;
import io.kadai.task.api.models.Task;
import io.kadai.task.internal.models.TaskImpl;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaskInformationMapperTest {

  private static final String CREATED = "2030-06-20T09:54:00.000+0000";
  private static final String PLANNED = "2030-06-25T09:54:00.000+0000";
  private static final String DUE = "2030-06-26T09:54:00.000+0000";

  private static final Instant EXPECTED_PLANNED =
      LocalDateTime.parse("2030-06-25T09:54:00")
          .atZone(ZoneId.systemDefault())
          .toInstant();

  private static final Instant EXPECTED_DUE =
      LocalDateTime.parse("2030-06-26T09:54:00")
          .atZone(ZoneId.systemDefault())
          .toInstant();

  private TaskService taskService;
  private TaskInformationMapper taskInformationMapper;

  @BeforeEach
  void setUp() {
    taskService = mock(TaskService.class);

    when(taskService.newTask("GPK_KSC", "DOMAIN_A"))
        .thenAnswer(ignored -> new TaskImpl());

    when(
            taskService.newObjectReference(
                "DEFAULT_COMPANY",
                "DEFAULT_SYSTEM",
                "DEFAULT_SYSTEM_INSTANCE",
                "DEFAULT_TYPE",
                "DEFAULT_VALUE"))
        .thenReturn(mock(ObjectReference.class));

    taskInformationMapper =
        new TaskInformationMapper(taskService, new KadaiSystemConnectorConfiguration());
  }

  @Test
  void should_SetPlannedToNow_When_DueAndPlannedAreMissing() {
    ReferencedTask referencedTask = createReferencedTask(null, null);

    Instant beforeConversion = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    Task kadaiTask = taskInformationMapper.convertToKadaiTask(referencedTask);
    Instant afterConversion = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    assertThat(kadaiTask.getDue()).isNull();
    assertThat(kadaiTask.getPlanned()).isNotNull();
    assertThat(kadaiTask.getPlanned()).isAfterOrEqualTo(beforeConversion);
    assertThat(kadaiTask.getPlanned()).isBeforeOrEqualTo(afterConversion);
  }

  @Test
  void should_KeepPlannedAndLeaveDueNull_When_OnlyPlannedIsProvided() {
    ReferencedTask referencedTask = createReferencedTask(PLANNED, null);

    Task kadaiTask = taskInformationMapper.convertToKadaiTask(referencedTask);

    assertThat(kadaiTask.getDue()).isNull();
    assertThat(kadaiTask.getPlanned()).isEqualTo(EXPECTED_PLANNED);
  }

  @Test
  void should_KeepDueAndLeavePlannedNull_When_OnlyDueIsProvided() {
    ReferencedTask referencedTask = createReferencedTask(null, DUE);

    Task kadaiTask = taskInformationMapper.convertToKadaiTask(referencedTask);

    assertThat(kadaiTask.getDue()).isEqualTo(EXPECTED_DUE);
    assertThat(kadaiTask.getPlanned()).isNull();
  }

  @Test
  void should_KeepDueAndPlanned_When_BothAreProvided() {
    ReferencedTask referencedTask = createReferencedTask(PLANNED, DUE);

    Task kadaiTask = taskInformationMapper.convertToKadaiTask(referencedTask);

    assertThat(kadaiTask.getDue()).isEqualTo(EXPECTED_DUE);
    assertThat(kadaiTask.getPlanned()).isEqualTo(EXPECTED_PLANNED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "null"})
  void should_KeepDueAndLeavePlannedNull_When_PlannedContainsMissingMarker(String planned) {
    ReferencedTask referencedTask = createReferencedTask(planned, DUE);

    Task kadaiTask = taskInformationMapper.convertToKadaiTask(referencedTask);

    assertThat(kadaiTask.getDue()).isEqualTo(EXPECTED_DUE);
    assertThat(kadaiTask.getPlanned()).isNull();
  }

  private ReferencedTask createReferencedTask(String planned, String due) {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId("external-task-id");
    referencedTask.setSystemUrl("http://camunda.example");
    referencedTask.setDomain("DOMAIN_A");
    referencedTask.setClassificationKey("L1050");
    referencedTask.setWorkbasketKey("GPK_KSC");
    referencedTask.setName("Test Task");
    referencedTask.setVariables("{}");
    referencedTask.setCreated(CREATED);
    referencedTask.setPlanned(planned);
    referencedTask.setDue(due);
    return referencedTask;
  }
}
