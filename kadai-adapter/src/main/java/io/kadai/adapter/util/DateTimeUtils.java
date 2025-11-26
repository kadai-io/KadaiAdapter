package io.kadai.adapter.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared helper class for date/time formatting and calculation of planned/due.
 */
public final class DateTimeUtils {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneId.systemDefault());

  private DateTimeUtils() {}

  public static String formatDate(Date date) {
    return date == null ? null : FORMATTER.format(date.toInstant());
  }

  public static String formatOffsetDateTime(OffsetDateTime odt) {
    return odt == null ? null : odt.format(FORMATTER);
  }

  private static <T> PlannedDue computePlannedDueGeneric(
      T followUp,
      T due,
      Function<T, String> formatter,
      Supplier<T> nowSupplier
  ) {
    boolean followUpSet = followUp != null;
    boolean dueSet = due != null;

    if (followUpSet && dueSet) {
      String planned = formatter.apply(followUp);
      String dueStr = formatter.apply(due);
      return new PlannedDue(planned, dueStr, true);
    } else if (followUpSet) {
      String planned = formatter.apply(followUp);
      return new PlannedDue(planned, null, false);
    } else if (dueSet) {
      String dueStr = formatter.apply(due);
      return new PlannedDue(null, dueStr, false);
    } else {
      String planned = formatter.apply(nowSupplier.get());
      return new PlannedDue(planned, null, false);
    }
  }

  public static PlannedDue computePlannedDue(Date followUp, Date due) {
    return computePlannedDueGeneric(
        followUp,
        due,
        DateTimeUtils::formatDate,
        Date::new
    );
  }

  public static PlannedDue computePlannedDue(OffsetDateTime followUp, OffsetDateTime due) {
    return computePlannedDueGeneric(
        followUp,
        due,
        DateTimeUtils::formatOffsetDateTime,
        OffsetDateTime::now
    );
  }

  public static final class PlannedDue {
    public final String planned;
    public final String due;
    public final boolean bothSet;

    public PlannedDue(String planned, String due, boolean bothSet) {
      this.planned = planned;
      this.due = due;
      this.bothSet = bothSet;
    }
  }
}
