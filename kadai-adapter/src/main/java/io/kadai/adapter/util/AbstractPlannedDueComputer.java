package io.kadai.adapter.util;

import io.kadai.adapter.model.PlannedDue;
import io.kadai.common.api.exceptions.SystemException;

/**
 * Generic base class that centralises the planned/due decision logic.
 *
 * @param <T> the temporal type used for followUp/due and for the "now" reference. Typical concrete
 *     types are {@link java.util.Date} or {@link java.time.OffsetDateTime}.
 */
public abstract class AbstractPlannedDueComputer<T> implements PlannedDueComputer<T> {

  protected final Formatter<T> formatter;

  protected AbstractPlannedDueComputer(Formatter<T> formatter) {
    this.formatter = formatter;
  }

  protected abstract T now();

  @Override
  public PlannedDue computePlannedDue(T followUp, T due, boolean enforce) {
    boolean followUpSet = followUp != null;
    boolean dueSet = due != null;

    String followUpFormatted = followUpSet ? formatter.format(followUp) : null;
    String dueFormatted = dueSet ? formatter.format(due) : null;
    String nowFormatted = formatter.format(now());

    if (followUpSet && dueSet && enforce) {
      throw new SystemException(
          "Both followUp and due dates are set. This is not allowed when "
              + "kadai.servicelevel.validation.enforce is true.");
    }

    if (followUpSet && dueSet) {
      return new PlannedDue(followUpFormatted, dueFormatted);
    } else if (followUpSet) {
      return new PlannedDue(followUpFormatted, null);
    } else if (dueSet) {
      return new PlannedDue(null, dueFormatted);
    } else {
      return new PlannedDue(nowFormatted, null);
    }
  }
}
