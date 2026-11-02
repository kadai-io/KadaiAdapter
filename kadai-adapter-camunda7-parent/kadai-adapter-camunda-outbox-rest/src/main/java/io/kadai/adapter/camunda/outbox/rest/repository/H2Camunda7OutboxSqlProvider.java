package io.kadai.adapter.camunda.outbox.rest.repository;

public class H2Camunda7OutboxSqlProvider implements Camunda7OutboxSqlProvider {

  // Compare bytes to preserve case-sensitive engine matching even with H2 IGNORECASE=TRUE.
  @Override
  public String getCreateEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? "
            + "and remaining_retries>0 and blocked_until < ? "
            + "and cast(system_engine_identifier as varbinary) = cast(? as varbinary) "
            + "fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCreateEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? and (lock_expire < ? or lock_expire is null) "
            + "and remaining_retries>0 and blocked_until < ? "
            + "fetch first %d rows only for update skip locked";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCreateEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? and (lock_expire < ? or lock_expire is null) "
            + "and remaining_retries>0 and blocked_until < ? "
            + "and cast(system_engine_identifier as varbinary) = cast(? as varbinary) "
            + "fetch first %d rows only for update skip locked";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAllAvailableEvents(String schema) {
    final String sql =
        "select * from %s.event_store where lock_expire < ? or lock_expire is null "
            + "for update skip locked";
    return String.format(sql, schema);
  }

  @Override
  public String getCompleteAndDeleteEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) "
            + "and cast(system_engine_identifier as varbinary) = cast(? as varbinary) "
            + "fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCompleteAndDeleteEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) and (lock_expire < ? or"
            + " lock_expire is null)"
            + " fetch first %d rows only for update skip locked";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCompleteAndDeleteEventsFilteredBySystemEngineId(
      String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) and (lock_expire < ? or"
            + " lock_expire is null)"
            + " and cast(system_engine_identifier as varbinary) = cast(? as varbinary)"
            + " fetch first %d rows only for update skip locked";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableEventsFilteredByRetries(String schema) {
    final String sql =
        "select * from %s.event_store where remaining_retries = ? and (lock_expire "
            + "< ? or lock_expire is null) for update skip locked";
    return String.format(sql, schema);
  }
}
