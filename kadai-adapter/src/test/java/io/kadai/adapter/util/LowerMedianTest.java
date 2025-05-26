package io.kadai.adapter.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LowerMedianTest {

  @Test
  void should_ReturnNothing_When_SampleIsEmpty() {
    final LowerMedian<Integer> lowerMedian = new LowerMedian<>(42);

    assertThat(lowerMedian.get()).isNotPresent();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584})
  void should_ReplaceOneItem_When_SampleReachedMaxSampleSize(int maxSampleSize) {
    final LowerMedian<Integer> lowerMedian = new LowerMedian<>(maxSampleSize);
    for (int i = 0; i < maxSampleSize; i++) {
      lowerMedian.add(i * i);
    }
    assertThat(lowerMedian.size()).isEqualTo(maxSampleSize);
    assertThat(lowerMedian.contains(42)).isFalse();

    lowerMedian.add(42);

    assertThat(lowerMedian.size()).isEqualTo(maxSampleSize);
    assertThat(lowerMedian.contains(42)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("unevenSampleProvider")
  <T extends Comparable<T>> void should_ReturnMedian_When_SampleSizeIsUneven(
      Class<T> clazz, List<T> sample, T expected) {
    final LowerMedian<T> lowerMedian = new LowerMedian<>(100);

    lowerMedian.addAll(sample);
    final Optional<T> actual = lowerMedian.get();

    assertThat(actual).isPresent();
    assertThat(actual.get()).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("evenSampleProvider")
  <T extends Comparable<T>> void should_ReturnRoundedCenter_When_SampleSizeIsEven(
      Class<T> clazz, List<T> sample, T expected) {
    final LowerMedian<T> lowerMedian = new LowerMedian<>(100);

    lowerMedian.addAll(sample);
    final Optional<T> actual = lowerMedian.get();

    assertThat(actual).isPresent();
    assertThat(actual.get()).isEqualTo(expected);
  }

  private static Stream<Arguments> unevenSampleProvider() {
    return Stream.of(
        Arguments.of(Integer.class, List.of(1, 2, 3, 4, 5), 3),
        Arguments.of(Integer.class, List.of(5, 4, 3, 2, 1), 3),
        Arguments.of(Integer.class, List.of(42), 42),
        Arguments.of(Integer.class, List.of(42, 7, 13), 13),
        Arguments.of(Double.class, List.of(1, 2, 3, 4, 5), 3),
        Arguments.of(Double.class, List.of(5, 4, 3, 2, 1), 3),
        Arguments.of(Double.class, List.of(42), 42),
        Arguments.of(Double.class, List.of(42, 7, 13), 13),
        Arguments.of(
            Duration.class,
            List.of(Duration.ofDays(1), Duration.ofHours(1), Duration.ofMinutes(1)),
            Duration.ofHours(1)));
  }

  private static Stream<Arguments> evenSampleProvider() {
    return Stream.of(
        Arguments.of(Integer.class, List.of(1, 2, 3, 4), 2),
        Arguments.of(Integer.class, List.of(5, 4, 3, 2), 3),
        Arguments.of(Integer.class, List.of(42, 100), 42),
        Arguments.of(Integer.class, List.of(13, 7), 7),
        Arguments.of(Double.class, List.of(1, 2, 3, 4), 2),
        Arguments.of(Double.class, List.of(5, 4, 3, 2), 3),
        Arguments.of(Double.class, List.of(42, 100), 42),
        Arguments.of(Double.class, List.of(13, 7), 7),
        Arguments.of(
            Duration.class, List.of(Duration.ofDays(42), Duration.ofDays(7)), Duration.ofDays(7)));
  }
}
