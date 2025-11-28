package io.kadai.adapter.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Concrete formatter for java.util.Date. Implements Formatter<Date> and delegates formatting to
 * DateTimeUtils.
 */
public class DateFormatter implements Formatter<Date> {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneId.systemDefault());

  @Override
  public String format(Date date) {
    return date == null ? null : FORMATTER.format(date.toInstant());
  }
}
