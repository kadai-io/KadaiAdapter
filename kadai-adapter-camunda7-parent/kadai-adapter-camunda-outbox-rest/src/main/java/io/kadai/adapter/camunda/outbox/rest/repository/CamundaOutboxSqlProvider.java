package io.kadai.adapter.camunda.outbox.rest.repository;

import io.kadai.common.api.exceptions.UnsupportedDatabaseException;
import io.kadai.common.internal.configuration.DB;

public interface CamundaOutboxSqlProvider {

  static CamundaOutboxSqlProvider valueOf(String databaseProductName) {
    if (databaseProductName.equals(DB.H2.dbProductName)) {
      return new H2CamundaOutboxSqlProvider();
    } else if (databaseProductName.equals(DB.DB2.dbProductName)) {
      return new Db2CamundaOutboxSqlProvider();
    } else if (databaseProductName.equals(DB.POSTGRES.dbProductName)) {
      return new PostgresCamundaOutboxSqlProvider();
    } else if (databaseProductName.contains("Oracle")) {
      return new OracleCamundaOutboxSqlProvider();
    } else {
      throw new UnsupportedDatabaseException(databaseProductName);
    }
  }

  default String getCreateEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? "
            + "and remaining_retries>0 and blocked_until < ? fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  String getAvailableCreateEvents(String schema, int maxRows);

  default String getAllEvents(String schema) {
    final String sql = "select * from %s.event_store";
    return String.format(sql, schema);
  }

  String getAllAvailableEvents(String schema);

  default String getEvent(String schema) {
    final String sql = "select * from %s.event_store where id = ? ";
    return String.format(sql, schema);
  }

  default String getCompleteAndDeleteEvents(String schema, int maxRows) {
    final String sql =
        "select * from %s.event_store where type = ? OR type = ? fetch first %d rows only";
    return String.format(sql, schema, maxRows);
  }

  String getAvailableCompleteAndDeleteEvents(String schema, int maxRows);

  default String getEventsFilteredByRetries(String schema) {
    final String sql = "select * from %s.event_store where remaining_retries = ?";
    return String.format(sql, schema);
  }

  String getAvailableEventsFilteredByRetries(String schema);

  default String getEventsCount(String schema) {
    final String sql = "select count(id) from %s.event_store where remaining_retries = ?";
    return String.format(sql, schema);
  }

  default String getSqlWithoutPlaceholdersDeleteEvents(String schema, String ids) {
    final String sql = "delete from %s.event_store where id in (%s)";
    return String.format(sql, schema, ids);
  }

  default String decreaseRemainingRetries(String schema) {
    final String sql =
        "update %s.event_store set remaining_retries = remaining_retries-1, blocked_until = ?, "
            + "error = ? where id = ?";
    return String.format(sql, schema);
  }

  default String setRemainingRetries(String schema) {
    final String sql = "update %s.event_store set remaining_retries = ? where id = ?";
    return String.format(sql, schema);
  }

  default String setRemainingRetriesForMultipleEvents(String schema) {
    final String sql =
        "update %s.event_store set remaining_retries = ? where remaining_retries = ?";
    return String.format(sql, schema);
  }

  default String deleteFailedEvent(String schema) {
    final String sql = "delete from %s.event_store where id = ? and remaining_retries <=0";
    return String.format(sql, schema);
  }

  default String deleteAllFailedEvents(String schema) {
    final String sql = "delete from %s.event_store where remaining_retries <= 0 ";
    return String.format(sql, schema);
  }

  default String setLockExpire(String schema, String ids) {
    final String sql = "update %s.event_store set lock_expire = ? where id in (%s)";
    return String.format(sql, schema, ids);
  }

  default String unlock(String schema, String ids) {
    final String sql = "update %s.event_store set lock_expire = null where id in (%s)";
    return String.format(sql, schema, ids);
  }
}
