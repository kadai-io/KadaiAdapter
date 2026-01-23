package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.UserTaskProperties;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class ReferencedTaskCreatorTest {

  @Test
  void should_CreateReferencedTaskFromActivatedJob_WhenNameIsGiven() {
    final Camunda8System camunda8System = new Camunda8System();
    camunda8System.setRestAddress("https://foo.bar.baz/bat");

    final ReferencedTask expected = new ReferencedTask();
    expected.setId("c8sysid-0-utk-123456789");
    expected.setAssignee("Holger");
    expected.setDue("2000-01-01T00:00:00.000+0000");
    expected.setPlanned("2001-01-01T00:00:00.000+0000");
    expected.setTaskDefinitionKey("abcdefgh");
    expected.setBusinessProcessId("987654321");
    expected.setSystemUrl("https://foo.bar.baz/bat");
    expected.setManualPriority("42");
    expected.setWorkbasketKey("foo");
    expected.setClassificationKey("bar");
    expected.setDomain("baz");
    expected.setName("bat");
    expected.setCustomInt1("1");
    expected.setCustomInt2("2");
    expected.setCustomInt3("3");
    expected.setCustomInt4("4");
    expected.setCustomInt5("5");
    expected.setCustomInt6("6");
    expected.setCustomInt7("7");
    expected.setCustomInt8("8");
    expected.setVariables("{\"meow\":\"meeh\",\"wuff\":\"37\",\"grrr\":\"girr\"}");

    final UserTaskProperties userTaskProperties = Mockito.mock(UserTaskProperties.class);
    when(userTaskProperties.getUserTaskKey()).thenReturn(123456789L);
    when(userTaskProperties.getAssignee()).thenReturn("Holger");
    when(userTaskProperties.getDueDate())
        .thenReturn(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(userTaskProperties.getFollowUpDate())
        .thenReturn(OffsetDateTime.of(2001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    final ActivatedJob activatedJob = Mockito.mock(ActivatedJob.class);
    when(activatedJob.getUserTask()).thenReturn(userTaskProperties);
    when(activatedJob.getElementId()).thenReturn("abcdefgh");
    when(activatedJob.getProcessInstanceKey()).thenReturn(987654321L);
    final HashMap<String, Object> variables = new HashMap<>();
    variables.put("kadai_manual_priority", 42);
    variables.put("kadai_workbasket_key", "foo");
    variables.put("kadai_classification_key", "bar");
    variables.put("kadai_domain", "baz");
    variables.put("kadai_name", "bat");
    variables.put("kadai_custom_int_1", 1);
    variables.put("kadai_custom_int_2", 2);
    variables.put("kadai_custom_int_3", 3);
    variables.put("kadai_custom_int_4", 4);
    variables.put("kadai_custom_int_5", 5);
    variables.put("kadai_custom_int_6", 6);
    variables.put("kadai_custom_int_7", 7);
    variables.put("kadai_custom_int_8", 8);
    variables.put("kadai_attributes", "meow,wuff,grrr");
    variables.put("meow", "meeh");
    variables.put("wuff", 37);
    variables.put("grrr", "girr");
    when(activatedJob.getVariablesAsMap()).thenReturn(variables);

    final ReferencedTaskCreator referencedTaskCreator = new ReferencedTaskCreator(camunda8System);
    final ReferencedTask actual = referencedTaskCreator.createReferencedTaskFromJob(activatedJob);

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "")
  void should_CreateReferencedTaskFromActivatedJobAndSetKadaiNameToDomain_WhenNameIsBlank(
      String kadaiName) {
    final Camunda8System camunda8System = new Camunda8System();
    camunda8System.setRestAddress("https://foo.bar.baz/bat");

    final ReferencedTask expected = new ReferencedTask();
    expected.setId("c8sysid-0-utk-123456789");
    expected.setAssignee("Holger");
    expected.setDue("2000-01-01T00:00:00.000+0000");
    expected.setPlanned("2001-01-01T00:00:00.000+0000");
    expected.setTaskDefinitionKey("abcdefgh");
    expected.setBusinessProcessId("987654321");
    expected.setSystemUrl("https://foo.bar.baz/bat");
    expected.setManualPriority("42");
    expected.setWorkbasketKey("foo");
    expected.setClassificationKey("bar");
    expected.setDomain("baz");
    expected.setName("baz");
    expected.setCustomInt1("1");
    expected.setCustomInt2("2");
    expected.setCustomInt3("3");
    expected.setCustomInt4("4");
    expected.setCustomInt5("5");
    expected.setCustomInt6("6");
    expected.setCustomInt7("7");
    expected.setCustomInt8("8");
    expected.setVariables("{\"meow\":\"meeh\",\"wuff\":\"37\",\"grrr\":\"girr\"}");

    final UserTaskProperties userTaskProperties = Mockito.mock(UserTaskProperties.class);
    when(userTaskProperties.getUserTaskKey()).thenReturn(123456789L);
    when(userTaskProperties.getAssignee()).thenReturn("Holger");
    when(userTaskProperties.getDueDate())
        .thenReturn(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(userTaskProperties.getFollowUpDate())
        .thenReturn(OffsetDateTime.of(2001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    final ActivatedJob activatedJob = Mockito.mock(ActivatedJob.class);
    when(activatedJob.getUserTask()).thenReturn(userTaskProperties);
    when(activatedJob.getElementId()).thenReturn("abcdefgh");
    when(activatedJob.getProcessInstanceKey()).thenReturn(987654321L);
    final HashMap<String, Object> variables = new HashMap<>();
    variables.put("kadai_manual_priority", 42);
    variables.put("kadai_workbasket_key", "foo");
    variables.put("kadai_classification_key", "bar");
    variables.put("kadai_domain", "baz");
    variables.put("kadai_name", kadaiName);
    variables.put("kadai_custom_int_1", 1);
    variables.put("kadai_custom_int_2", 2);
    variables.put("kadai_custom_int_3", 3);
    variables.put("kadai_custom_int_4", 4);
    variables.put("kadai_custom_int_5", 5);
    variables.put("kadai_custom_int_6", 6);
    variables.put("kadai_custom_int_7", 7);
    variables.put("kadai_custom_int_8", 8);
    variables.put("kadai_attributes", "meow,wuff,grrr");
    variables.put("meow", "meeh");
    variables.put("wuff", 37);
    variables.put("grrr", "girr");
    when(activatedJob.getVariablesAsMap()).thenReturn(variables);

    final ReferencedTaskCreator referencedTaskCreator = new ReferencedTaskCreator(camunda8System);
    final ReferencedTask actual = referencedTaskCreator.createReferencedTaskFromJob(activatedJob);

    assertThat(actual).isEqualTo(expected);
  }

  @MethodSource("splitVariableNamesProvider")
  @ParameterizedTest
  void should_SplitVariableNames(String concatenatedVariableNames, List<String> expected) {
    final List<String> actual =
        ReferencedTaskCreator.splitVariableNames(concatenatedVariableNames, ',');

    assertThat(actual).isEqualTo(expected);
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
}
