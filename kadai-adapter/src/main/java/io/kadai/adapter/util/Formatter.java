package io.kadai.adapter.util;

/**
 * Generic formatting abstraction for a single value type.
 */
public interface Formatter<T> {
  String format(T value);
}

