package io.kadai.adapter.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Concrete formatter for java.time.OffsetDateTime. Implements Formatter<OffsetDateTime> and
 * delegates formatting to DateTimeUtils.
 */
public class OffsetDateTimeFormatter implements Formatter<OffsetDateTime> {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneId.systemDefault());

  @Override
  public String format(OffsetDateTime odt) {
    return odt == null ? null : odt.format(FORMATTER);
  }
}
