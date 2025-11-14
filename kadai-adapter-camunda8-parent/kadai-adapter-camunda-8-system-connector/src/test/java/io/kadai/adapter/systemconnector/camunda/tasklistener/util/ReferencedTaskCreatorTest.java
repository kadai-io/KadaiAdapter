package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReferencedTaskCreatorTest {

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
