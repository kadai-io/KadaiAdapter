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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that clears the test databases between test runs.
 *
 * <p>Executes the SQL scripts from {@code sql/clear-kadai-db.sql}, {@code
 * sql/clear-camunda-db.sql}, or {@code sql/clear-outbox-db-postgres.sql} depending on the {@link
 * ApplicationDatabaseType}.
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
    String sqlFile =
        switch (type) {
          case KADAI -> "sql/clear-kadai-db.sql";
          case CAMUNDA -> "sql/clear-camunda-db.sql";
          case OUTBOX -> "sql/clear-outbox-db-postgres.sql";
        };

    List<String> statements = readSqlStatements(sqlFile);
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      for (String sql : statements) {
        String trimmed = sql.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
          try (Statement stmt = conn.createStatement()) {
            LOGGER.debug("Executing: {}", trimmed);
            stmt.executeUpdate(trimmed);
          } catch (SQLException e) {
            // Ignore "table does not exist" errors (the table may not have been created yet)
            LOGGER.warn("Ignoring SQL error while clearing db: {}", e.getMessage());
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

  public enum ApplicationDatabaseType {
    KADAI,
    CAMUNDA,
    OUTBOX
  }
}
