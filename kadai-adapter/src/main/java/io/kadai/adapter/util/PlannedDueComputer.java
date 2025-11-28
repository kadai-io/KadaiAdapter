package io.kadai.adapter.util;

import io.kadai.adapter.model.PlannedDue;

/**
 * Generic interface: computes PlannedDue for a given followUp/due type.
 */
public interface PlannedDueComputer<T> {
  PlannedDue computePlannedDue(T followUp, T due, boolean enforce);
}
