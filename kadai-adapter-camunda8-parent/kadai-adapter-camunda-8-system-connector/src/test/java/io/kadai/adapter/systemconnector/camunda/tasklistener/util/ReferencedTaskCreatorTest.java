package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ReferencedTaskCreatorTest {

  @CsvSource(
      delimiter = ',',
      value = {
        "c8sysid-alpha-utk-54187487447-eik-4281741874, 54187487447",
        "c8sysid-beta-utk-54187487447-eik-5427894827, 54187487447",
        "c8sysid-451507417441-utk-54187487447-eik-45414714785142, 54187487447",
        "c8sysid-0-utk-54187487447-eik-54771478745, 54187487447",
        "c8sysid-1-utk-54187487447-eik-1238745125, 54187487447",
      })
  @ParameterizedTest
  void should_ExtractUserTaskKeyFromTaskId(String taskId, Long expectedUserTaskKey) {
    final Long actual = ReferencedTaskCreator.extractUserTaskKeyFromTaskId(taskId);

    assertThat(actual).isEqualTo(expectedUserTaskKey);
  }

  @CsvSource(
      delimiter = ',',
      value = {
        "c8sysid-alpha-utk-54187487447-eik-4281741874, 4281741874",
        "c8sysid-beta-utk-54187487447-eik-5427894827, 5427894827",
        "c8sysid-451507417441-utk-54187487447-eik-45414714785142, 45414714785142",
        "c8sysid-0-utk-54187487447-eik-54771478745, 54771478745",
        "c8sysid-1-utk-54187487447-eik-1238745125, 1238745125",
      })
  @ParameterizedTest
  void should_ExtractElementInstanceKeyFromTaskId(String taskId, Long expectedElementInstanceKey) {
    final Long actual = ReferencedTaskCreator.extractElementInstanceKeyFromTaskId(taskId);

    assertThat(actual).isEqualTo(expectedElementInstanceKey);
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
