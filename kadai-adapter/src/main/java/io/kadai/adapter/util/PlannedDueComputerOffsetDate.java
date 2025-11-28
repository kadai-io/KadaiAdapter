package io.kadai.adapter.util;

import io.kadai.adapter.model.PlannedDue;
import io.kadai.common.api.exceptions.SystemException;
import java.time.OffsetDateTime;

/**
 * PlannedDueComputer implementation for java.time.OffsetDateTime. Mirrors the logic of
 * PlannedDueComputerDate but for OffsetDateTime inputs.
 */
public final class PlannedDueComputerOffsetDate implements PlannedDueComputer<OffsetDateTime> {

  private final Formatter<OffsetDateTime> formatter = new OffsetDateTimeFormatter();

  @Override
  public PlannedDue computePlannedDue(
      OffsetDateTime followUp, OffsetDateTime due, boolean enforce) {
    boolean followUpSet = followUp != null;
    boolean dueSet = due != null;

    String followUpFormatted = followUpSet ? formatter.format(followUp) : null;
    String dueFormatted = dueSet ? formatter.format(due) : null;
    String nowFormatted = formatter.format(OffsetDateTime.now());

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
