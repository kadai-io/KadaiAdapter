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

package io.kadai.adapter.camunda.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.camunda.CamundaListenerConfiguration;
import io.kadai.adapter.camunda.exceptions.SystemException;
import io.kadai.adapter.camunda.mapper.JacksonConfigurator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Test for KadaiTaskListener date logic. */
class KadaiTaskListenerLogicTest {

  private final ObjectMapper objectMapper = JacksonConfigurator.createAndConfigureObjectMapper();

  @Test
  void should_SetPlannedToNowAndDueToNull_When_NeitherFollowUpNorDueAreSet() throws Exception {
    DelegateTask delegateTask = createMockDelegateTask(null, null);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    String referencedTaskJson = invokeGetReferencedTaskJson(listener, delegateTask);
    JsonNode jsonNode = objectMapper.readTree(referencedTaskJson);

    assertThat(jsonNode.get("planned").asText()).isNotNull();
    assertThat(jsonNode.get("due").isNull()).isTrue();
  }

  @Test
  void should_SetPlannedToFollowUpAndDueToNull_When_OnlyFollowUpIsSet() throws Exception {
    Date followUpDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    DelegateTask delegateTask = createMockDelegateTask(followUpDate, null);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    String referencedTaskJson = invokeGetReferencedTaskJson(listener, delegateTask);
    JsonNode jsonNode = objectMapper.readTree(referencedTaskJson);

    String expectedPlanned =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .withZone(ZoneId.systemDefault())
            .format(followUpDate.toInstant());
    assertThat(jsonNode.get("planned").asText()).isEqualTo(expectedPlanned);
    assertThat(jsonNode.get("due").isNull()).isTrue();
  }

  @Test
  void should_SetPlannedToNullAndDueToDate_When_OnlyDueIsSet() throws Exception {
    Date dueDate = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
    DelegateTask delegateTask = createMockDelegateTask(null, dueDate);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    String referencedTaskJson = invokeGetReferencedTaskJson(listener, delegateTask);
    JsonNode jsonNode = objectMapper.readTree(referencedTaskJson);

    String expectedDue =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .withZone(ZoneId.systemDefault())
            .format(dueDate.toInstant());
    assertThat(jsonNode.get("planned").isNull()).isTrue();
    assertThat(jsonNode.get("due").asText()).isEqualTo(expectedDue);
  }

  @Test
  void should_SetBothPlannedAndDue_When_BothAreSetAndEnforcementIsFalse() throws Exception {
    Date followUpDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    Date dueDate = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
    DelegateTask delegateTask = createMockDelegateTask(followUpDate, dueDate);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    try (MockedStatic<CamundaListenerConfiguration> mockedConfig =
        mockStatic(CamundaListenerConfiguration.class)) {
      mockedConfig
          .when(CamundaListenerConfiguration::shouldEnforceServiceLevelValidation)
          .thenReturn(false);

      String referencedTaskJson = invokeGetReferencedTaskJson(listener, delegateTask);
      JsonNode jsonNode = objectMapper.readTree(referencedTaskJson);

      String expectedDue =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
              .withZone(ZoneId.systemDefault())
              .format(dueDate.toInstant());
      String expectedPlanned =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
              .withZone(ZoneId.systemDefault())
              .format(followUpDate.toInstant());
      assertThat(jsonNode.get("planned").asText()).isEqualTo(expectedPlanned);
      assertThat(jsonNode.get("due").asText()).isEqualTo(expectedDue);
    }
  }

  @Test
  void should_ThrowSystemException_When_BothAreSetAndEnforcementIsTrue() throws Exception {
    Date followUpDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    Date dueDate = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
    DelegateTask delegateTask = createMockDelegateTask(followUpDate, dueDate);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    try (MockedStatic<CamundaListenerConfiguration> mockedConfig =
        mockStatic(CamundaListenerConfiguration.class)) {
      mockedConfig
          .when(CamundaListenerConfiguration::shouldEnforceServiceLevelValidation)
          .thenReturn(true);

      assertThatThrownBy(() -> invokeGetReferencedTaskJson(listener, delegateTask))
          .isInstanceOf(SystemException.class)
          .hasMessageContaining("Both followUp and due dates are set")
          .hasMessageContaining("kadai.servicelevel.validation.enforce");
    }
  }

  private DelegateTask createMockDelegateTask(Date followUpDate, Date dueDate) {
    DelegateTask delegateTask = mock(DelegateTask.class);
    ProcessEngine processEngine = mock(ProcessEngine.class);
    ExecutionEntity execution = mock(ExecutionEntity.class);
    BpmnModelInstance bpmnModelInstance = mock(BpmnModelInstance.class);
    UserTask userTask = mock(UserTask.class);

    when(delegateTask.getId()).thenReturn("task-123");
    when(delegateTask.getProcessEngine()).thenReturn(processEngine);
    when(processEngine.getName()).thenReturn("testEngine");
    when(delegateTask.getCreateTime()).thenReturn(new Date());
    when(delegateTask.getPriority()).thenReturn(50);
    when(delegateTask.getName()).thenReturn("Test Task");
    when(delegateTask.getFollowUpDate()).thenReturn(followUpDate);
    when(delegateTask.getDueDate()).thenReturn(dueDate);
    when(delegateTask.getExecution()).thenReturn(execution);
    when(execution.getBpmnModelInstance()).thenReturn(bpmnModelInstance);
    when(execution.getBpmnModelElementInstance()).thenReturn(userTask);

    return delegateTask;
  }

  private String invokeGetReferencedTaskJson(KadaiTaskListener listener, DelegateTask delegateTask)
      throws Exception {
    Method method =
        KadaiTaskListener.class.getDeclaredMethod("getReferencedTaskJson", DelegateTask.class);
    method.setAccessible(true);
    try {
      return (String) method.invoke(listener, delegateTask);
    } catch (InvocationTargetException e) {
      // Unwrap the InvocationTargetException so the test receives the original exception (e.g.
      // SystemException)
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      } else {
        throw new RuntimeException(cause);
      }
    }
  }
}
