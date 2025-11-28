package io.kadai.adapter.util;

import io.kadai.adapter.model.PlannedDue;
import io.kadai.common.api.exceptions.SystemException;
import java.util.Date;


/**
 * PlannedDueComputer implementation for java.util.Date.
 * Decision logic and formatting are encapsulated here.
 */
public final class PlannedDueComputerDate implements PlannedDueComputer<Date> {

  private final Formatter<Date> formatter = new DateFormatter();

  @Override
  public PlannedDue computePlannedDue(Date followUp, Date due, boolean enforce) {
    boolean followUpSet = followUp != null;
    boolean dueSet = due != null;

    // format inputs up-front
    String followUpFormatted = followUpSet ? formatter.format(followUp) : null;
    String dueFormatted = dueSet ? formatter.format(due) : null;
    String nowFormatted = formatter.format(new Date());

    // centralized enforcement check (business logic)
    if (followUpSet && dueSet && enforce) {
      throw new SystemException(
          "Both followUp and due dates are set. This is not allowed when "
              + "kadai.servicelevel.validation.enforce is true.");
    }

    // explicit if / else-if / else cascade (four cases)
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
