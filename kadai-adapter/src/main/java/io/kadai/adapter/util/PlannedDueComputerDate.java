package io.kadai.adapter.util;

import java.util.Date;

/** PlannedDueComputer implementation for java.util.Date. */
public final class PlannedDueComputerDate extends AbstractPlannedDueComputer<Date> {

  public PlannedDueComputerDate() {
    super(new DateFormatter());
  }

  @Override
  protected Date now() {
    return new Date();
  }
}
