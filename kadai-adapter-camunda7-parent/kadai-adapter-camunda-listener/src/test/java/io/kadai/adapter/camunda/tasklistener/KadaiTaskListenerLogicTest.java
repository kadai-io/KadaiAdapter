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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Stream;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/** Test for setting planned and due in KadaiTaskListener. */
class KadaiTaskListenerLogicTest {

  private final ObjectMapper objectMapper = JacksonConfigurator.createAndConfigureObjectMapper();

  @MethodSource("provideListenerCases")
  @ParameterizedTest
  void should_HandlePlannedAndDueCases(
      Date followUpDate,
      Date dueDate,
      boolean enforceServiceLevelValidation,
      boolean expectException)
      throws Exception {

    DelegateTask delegateTask = createMockDelegateTask(followUpDate, dueDate);
    KadaiTaskListener listener = KadaiTaskListener.getInstance();

    try (MockedStatic<CamundaListenerConfiguration> mockedConfig =
        mockStatic(CamundaListenerConfiguration.class)) {
      mockedConfig
          .when(CamundaListenerConfiguration::shouldEnforceServiceLevelValidation)
          .thenReturn(enforceServiceLevelValidation);

      if (expectException) {
        assertThatThrownBy(() -> invokeGetReferencedTaskJson(listener, delegateTask))
            .isInstanceOf(SystemException.class)
            .hasMessageContaining("Both followUp and due dates are set")
            .hasMessageContaining("kadai.servicelevel.validation.enforce");
        return;
      }

      boolean expectPlannedNotNull = !(followUpDate == null && dueDate != null);
      boolean expectDueNotNull = (dueDate != null);

      String referencedTaskJson = invokeGetReferencedTaskJson(listener, delegateTask);
      JsonNode jsonNode = objectMapper.readTree(referencedTaskJson);

      if (expectPlannedNotNull) {
        String plannedText = jsonNode.get("planned").asText();
        Instant actualPlanned = parseAndNormalizeInstant(plannedText);
        Instant expectedPlanned = (followUpDate == null) ? Instant.now() : followUpDate.toInstant();
        if (followUpDate != null) {
          assertInstantsClose(expectedPlanned, actualPlanned);
        }
      } else {
        assertThat(jsonNode.get("planned").isNull()).isTrue();
      }

      if (expectDueNotNull) {
        String dueText = jsonNode.get("due").asText();
        Instant actualDue = parseAndNormalizeInstant(dueText);
        Instant expectedDue = dueDate.toInstant();
        assertInstantsClose(expectedDue, actualDue);
      } else {
        assertThat(jsonNode.get("due").isNull()).isTrue();
      }
    }
  }

  private static Stream<Arguments> provideListenerCases() {
    Date followUp = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    Date due = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
    return Stream.of(
        Arguments.of(null, null, false, false),
        Arguments.of(followUp, null, false, false),
        Arguments.of(null, due, false, false),
        Arguments.of(followUp, due, false, false),
        Arguments.of(followUp, due, true, true));
  }

  private static Instant parseAndNormalizeInstant(String s) {
    if (s == null) {
      return null;
    }
    String normalized = s;
    if (s.matches(".+[+-]\\d{4}$")) {
      normalized = s.substring(0, s.length() - 2) + ":" + s.substring(s.length() - 2);
    }
    try {
      return java.time.OffsetDateTime.parse(normalized).toInstant();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Cannot parse temporal string: " + s, e);
    }
  }

  private static void assertInstantsClose(Instant expected, Instant actual) {
    long diffMillis = Math.abs(java.time.Duration.between(expected, actual).toMillis());
    assertThat(diffMillis)
        .withFailMessage(
            "Expected instants to be within 1000ms but difference was %d ms", diffMillis)
        .isLessThanOrEqualTo(1000);
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
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      } else {
        throw new RuntimeException(cause);
      }
    }
  }
}
