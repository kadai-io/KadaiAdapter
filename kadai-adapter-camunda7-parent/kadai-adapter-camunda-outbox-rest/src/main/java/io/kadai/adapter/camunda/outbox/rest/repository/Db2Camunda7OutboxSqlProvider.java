package io.kadai.adapter.camunda.outbox.rest.repository;

public class Db2Camunda7OutboxSqlProvider implements Camunda7OutboxSqlProvider {

  private static final String LOCK_CLAUSE = " for read only with rs use and keep update locks";

  // Binary comparison preserves Java's case and trailing-space semantics regardless of collation.
  // Type the parameter as VARCHAR first so JDBC binds text, not a binary value.
  private static final String ENGINE_FILTER =
      "and varbinary(system_engine_identifier) = varbinary(cast(? as varchar(32672 octets))) ";

  @Override
  public String getCreateEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? "
            + "and remaining_retries>0 and blocked_until < ? "
            + ENGINE_FILTER
            + "fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCreateEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? and (lock_expire < ? or lock_expire is null) "
            + "and remaining_retries>0 and blocked_until < ? "
            + "fetch first %d rows only"
            + LOCK_CLAUSE;
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCreateEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? and (lock_expire < ? or lock_expire is null) "
            + "and remaining_retries>0 and blocked_until < ? "
            + ENGINE_FILTER
            + "fetch first %d rows only"
            + LOCK_CLAUSE;
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAllAvailableEvents(String schema) {
    final String sql =
        "select * from %s.event_store where lock_expire < ? or lock_expire is null " + LOCK_CLAUSE;
    return String.format(sql, schema);
  }

  @Override
  public String getCompleteAndDeleteEventsFilteredBySystemEngineId(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) "
            + ENGINE_FILTER
            + "fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCompleteAndDeleteEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) and (lock_expire < ? or"
            + " lock_expire is null)"
            + " fetch first %d rows only"
            + LOCK_CLAUSE;
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableCompleteAndDeleteEventsFilteredBySystemEngineId(
      String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where (type = ? OR type = ?) and (lock_expire < ? or"
            + " lock_expire is null) "
            + ENGINE_FILTER
            + "fetch first %d rows only"
            + LOCK_CLAUSE;
    return String.format(sql, schema, maxRows);
  }

  @Override
  public String getAvailableEventsFilteredByRetries(String schema) {
    final String sql =
        "select * from %s.event_store where remaining_retries = ? and (lock_expire "
            + "< ? or lock_expire is null)"
            + LOCK_CLAUSE;
    return String.format(sql, schema);
  }
}
