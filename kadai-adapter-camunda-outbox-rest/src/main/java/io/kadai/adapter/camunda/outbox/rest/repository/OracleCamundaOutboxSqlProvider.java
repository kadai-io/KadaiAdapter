package io.kadai.adapter.camunda.outbox.rest.repository;

public class OracleCamundaOutboxSqlProvider implements CamundaOutboxSqlProvider {

  @Override
  public String getAvailableCreateEvents(String schema, int maxRows) {
    final String sql =
        "SELECT * FROM %s.event_store WHERE ROWID IN ("
            + "SELECT ROWID FROM %s.event_store WHERE type = ? "
            + "and (lock_expire < ? or lock_expire is null) "
            + "and remaining_retries>0 and "
            + "blocked_until < ? "
            + "fetch first %d rows only"
            + ")"
            + "FOR UPDATE SKIP LOCKED";
    return String.format(sql, schema, schema, maxRows);
  }

  @Override
  public String getAllAvailableEvents(String schema) {
    final String sql =
        "select * from %s.event_store where lock_expire < ? or lock_expire is null for update skip locked";
    return String.format(sql, schema);
  }

  @Override
  public String getAvailableCompleteAndDeleteEvents(String schema, int maxRows) {
    final String sql =
        "SELECT * FROM %s.event_store WHERE ROWID IN ("
            + "SELECT ROWID FROM %s.event_store WHERE "
            + "(type = ? OR type = ?)"
            + "and (lock_expire < ? or lock_expire is null) "
            + "fetch first %d rows only"
            + ")"
            + "FOR UPDATE SKIP LOCKED";
    return String.format(sql, schema, schema, maxRows);
  }

  @Override
  public String getAvailableEventsFilteredByRetries(String schema) {
    final String sql =
        "select * from %s.event_store where remaining_retries = ? and lock_expire "
            + "< ? or lock_expire is null for update skip locked";
    return String.format(sql, schema);
  }
}
