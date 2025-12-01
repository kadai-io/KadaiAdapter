package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.UserTaskProperties;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.common.api.exceptions.SystemException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class ReferencedTaskCreatorTest {

  @MethodSource("splitVariableNamesProvider")
  @ParameterizedTest
  void should_SplitVariableNames(String concatenatedVariableNames, List<String> expected) {
    final List<String> actual =
        ReferencedTaskCreator.splitVariableNames(concatenatedVariableNames, ',');

    assertThat(actual).isEqualTo(expected);
  }

  @MethodSource("nonExceptionCasesProvider")
  @ParameterizedTest
  void should_HandlePlannedAndDueNonExceptionCases(OffsetDateTime followUp, OffsetDateTime due) {
    Camunda8System camunda8System = createMockCamunda8System();
    ReferencedTaskCreator creator = new ReferencedTaskCreator(camunda8System);
    ReflectionTestUtils.setField(creator, "enforceServiceLevelValidation", false);

    ActivatedJob job = createMockActivatedJob(followUp, due);
    ReferencedTask referencedTask = creator.createReferencedTaskFromJob(job);

    if (followUp == null && due == null) {
      assertThat(referencedTask.getPlanned()).isNotNull();
      assertThat(referencedTask.getDue()).isNull();
    } else {
      if (followUp != null) {
        assertThat(referencedTask.getPlanned()).isNotNull();
        assertInstantsClose(followUp.toInstant(), toInstant(referencedTask.getPlanned()));
      } else {
        assertThat(referencedTask.getPlanned()).isNull();
      }

      if (due != null) {
        assertThat(referencedTask.getDue()).isNotNull();
        assertInstantsClose(due.toInstant(), toInstant(referencedTask.getDue()));
      } else {
        assertThat(referencedTask.getDue()).isNull();
      }
    }
  }

  @Test
  void should_ThrowSystemException_When_BothAreSetAndEnforcementIsTrue() {
    Camunda8System camunda8System = createMockCamunda8System();
    ReferencedTaskCreator creator = new ReferencedTaskCreator(camunda8System);
    ReflectionTestUtils.setField(creator, "enforceServiceLevelValidation", true);

    OffsetDateTime followUpDate = OffsetDateTime.now().plus(1, ChronoUnit.DAYS);
    OffsetDateTime dueDate = OffsetDateTime.now().plus(2, ChronoUnit.DAYS);
    ActivatedJob job = createMockActivatedJob(followUpDate, dueDate);

    assertThatThrownBy(() -> creator.createReferencedTaskFromJob(job))
        .isInstanceOf(SystemException.class)
        .hasMessageContaining("Both followUp and due dates are set")
        .hasMessageContaining("kadai.servicelevel.validation.enforce");
  }

  private static Stream<Arguments> splitVariableNamesProvider() {
    return Stream.of(
        Arguments.of("foo", List.of("foo")),
        Arguments.of("foo ", List.of("foo")),
        Arguments.of("foo, bar", List.of("foo", "bar")),
        Arguments.of("foo, bar  ", List.of("foo", "bar")),
        Arguments.of("foo, bar, baz", List.of("foo", "bar", "baz")),
        Arguments.of(" foo, bar, baz", List.of("foo", "bar", "baz")),
        Arguments.of(" foo, bar, baz, bat ", List.of("foo", "bar", "baz", "bat")),
        Arguments.of("", List.of()),
        Arguments.of(" ", List.of()));
  }

  private static Stream<Arguments> nonExceptionCasesProvider() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(OffsetDateTime.now().plus(1, ChronoUnit.DAYS), null),
        Arguments.of(null, OffsetDateTime.now().plus(2, ChronoUnit.DAYS)),
        Arguments.of(
            OffsetDateTime.now().plus(1, ChronoUnit.DAYS),
            OffsetDateTime.now().plus(2, ChronoUnit.DAYS)));
  }

  private Camunda8System createMockCamunda8System() {
    Camunda8System camunda8System = mock(Camunda8System.class);
    when(camunda8System.getIdentifier()).thenReturn(1);
    when(camunda8System.getRestAddress()).thenReturn("http://localhost:8080");
    return camunda8System;
  }

  private ActivatedJob createMockActivatedJob(OffsetDateTime followUpDate, OffsetDateTime dueDate) {
    ActivatedJob job = mock(ActivatedJob.class);
    UserTaskProperties userTaskProperties = mock(UserTaskProperties.class);

    when(job.getUserTask()).thenReturn(userTaskProperties);
    when(userTaskProperties.getUserTaskKey()).thenReturn(12345L);
    when(userTaskProperties.getFollowUpDate()).thenReturn(followUpDate);
    when(userTaskProperties.getDueDate()).thenReturn(dueDate);
    when(userTaskProperties.getAssignee()).thenReturn("testUser");

    when(job.getElementId()).thenReturn("testTaskId");
    when(job.getBpmnProcessId()).thenReturn("testProcessId");
    when(job.getVariablesAsMap()).thenReturn(Collections.emptyMap());

    return job;
  }

  private static Instant toInstant(Object temporal) {
    if (temporal == null) {
      return null;
    }
    if (!(temporal instanceof CharSequence)) {
      throw new IllegalArgumentException(
          "Expected temporal as CharSequence (String) but was: " + temporal.getClass());
    }

    String s = temporal.toString();

    if (s.matches(".+[+-]\\d{4}$")) {
      s = s.substring(0, s.length() - 2) + ":" + s.substring(s.length() - 2);
    }

    try {
      return OffsetDateTime.parse(s).toInstant();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Cannot parse temporal string: " + s + " : " + e.getMessage(), e);
    }
  }

  private static void assertInstantsClose(Instant expected, Instant actual) {
    assertThat(actual).isNotNull();
    long diffMillis = Math.abs(Duration.between(expected, actual).toMillis());
    assertThat(diffMillis)
        .withFailMessage(
            "Expected instants to be within 1000ms but difference was %d ms", diffMillis)
        .isLessThanOrEqualTo(1000);
  }
}
