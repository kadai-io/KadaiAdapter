package io.kadai.adapter.util;

import java.time.OffsetDateTime;

/** PlannedDueComputer implementation for java.time.OffsetDateTime. */
public final class PlannedDueComputerOffsetDate extends AbstractPlannedDueComputer<OffsetDateTime> {

  public PlannedDueComputerOffsetDate() {
    super(new OffsetDateTimeFormatter());
  }

  @Override
  protected OffsetDateTime now() {
    return OffsetDateTime.now();
  }
}
