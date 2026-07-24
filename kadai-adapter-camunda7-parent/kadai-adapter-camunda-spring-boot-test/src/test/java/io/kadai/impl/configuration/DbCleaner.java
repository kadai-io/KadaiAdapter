/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.impl.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that clears the test databases between test runs.
 *
 * <p>Executes the SQL scripts from {@code sql/clear-kadai-db.sql} or {@code
 * sql/clear-camunda-db.sql}. For the outbox database it uses the runtime
 * {@code kadai.adapter.outbox.schema} property because the schema differs across H2, Postgres,
 * Oracle, and DB2.
 */
public class DbCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbCleaner.class);

  /**
   * Executes the appropriate SQL script against the supplied {@link DataSource}.
   *
   * @param dataSource the JDBC data source to clear
   * @param type which application's tables to delete
   * @throws RuntimeException if a top-level SQLException prevents the cleanup
   */
  public void clearDb(DataSource dataSource, ApplicationDatabaseType type) {
    List<String> statements =
        switch (type) {
          case KADAI -> readSqlStatements("sql/clear-kadai-db.sql");
          case CAMUNDA -> readSqlStatements("sql/clear-camunda-db.sql");
          case OUTBOX -> List.of("DELETE FROM " + getOutboxSchema() + ".EVENT_STORE");
        };
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      for (String sql : statements) {
        String trimmed = sql.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
          Savepoint savepoint = conn.setSavepoint();
          try (Statement stmt = conn.createStatement()) {
            LOGGER.debug("Executing: {}", trimmed);
            stmt.executeUpdate(trimmed);
          } catch (SQLException e) {
            if (isMissingTableError(e)) {
              conn.rollback(savepoint);
              LOGGER.warn("Ignoring missing table while clearing db: {}", e.getMessage());
            } else {
              throw new RuntimeException("Failed to clear database (" + type + ")", e);
            }
          }
        }
      }
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to clear database (" + type + ")", e);
    }
  }

  private List<String> readSqlStatements(String resource) {
    List<String> stmts = new ArrayList<>();
    try (InputStream is =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      StringBuilder current = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.startsWith("--") || trimmed.isEmpty()) {
          continue;
        }
        current.append(" ").append(trimmed);
        if (trimmed.endsWith(";")) {
          // strip trailing semicolon (JDBC does not need it and some drivers reject it)
          String stmt = current.toString().trim();
          if (stmt.endsWith(";")) {
            stmt = stmt.substring(0, stmt.length() - 1);
          }
          stmts.add(stmt);
          current.setLength(0);
        }
      }
      // flush any remaining
      String rem = current.toString().trim();
      if (!rem.isEmpty()) {
        stmts.add(rem);
      }
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException("Failed to read SQL script: " + resource, e);
    }
    return stmts;
  }

  private boolean isMissingTableError(SQLException e) {
    return "42P01".equals(e.getSQLState()) // Postgres
        || "42S02".equals(e.getSQLState()) // H2
        || "42704".equals(e.getSQLState()) // DB2
        || e.getErrorCode() == 942 // Oracle
        || e.getErrorCode() == -204 // DB2
        || e.getErrorCode() == 42102; // H2
  }

  private String getOutboxSchema() {
    return System.getProperty("kadai.adapter.outbox.schema", "kadai_tables");
  }

  public enum ApplicationDatabaseType {
    KADAI,
    CAMUNDA,
    OUTBOX
  }
}
