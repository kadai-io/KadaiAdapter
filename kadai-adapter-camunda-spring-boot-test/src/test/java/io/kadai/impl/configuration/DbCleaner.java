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

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class to clean up databases. Can be used to clean Camunda or KADAI database */
public class DbCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbCleaner.class);
  private static final String KADAI_DB_CLEAR_SCRIPT = "/sql/clear-kadai-db.sql";
  private static final String KADAI_ADAPTER_DB_CLEAR_SCRIPT = "/sql/clear-kadai-adapter-db.sql";
  private static final String CAMUNDA_DB_CLEAR_SCRIPT = "/sql/clear-camunda-db.sql";
  private static final String OUTBOX_DB_CLEAR_SCRIPT = "/sql/clear-outbox-db.sql";
  private static final String KADAI_ADAPTER_DB_CLEAR_POSTGRES =
      "/sql/clear-kadai-adapter-db-postgres.sql";
  private static final String OUTBOX_DB_CLEAR_POSTGRES = "/sql/clear-outbox-db-postgres.sql";
  private final Map<ApplicationDatabaseType, String> typeScriptMap =
      new HashMap<ApplicationDatabaseType, String>();
  private final Map<ApplicationDatabaseType, String> typeScriptMapPostgres =
      new HashMap<ApplicationDatabaseType, String>();
  private final StringWriter outWriter = new StringWriter();
  private final PrintWriter logWriter = new PrintWriter(outWriter);
  private final StringWriter errorWriter = new StringWriter();
  private final PrintWriter errorLogWriter = new PrintWriter(errorWriter);

  public DbCleaner() {
    this.typeScriptMap.put(ApplicationDatabaseType.KADAI, KADAI_DB_CLEAR_SCRIPT);
    this.typeScriptMap.put(ApplicationDatabaseType.KADAI_ADAPTER, KADAI_ADAPTER_DB_CLEAR_SCRIPT);
    this.typeScriptMap.put(ApplicationDatabaseType.CAMUNDA, CAMUNDA_DB_CLEAR_SCRIPT);
    this.typeScriptMap.put(ApplicationDatabaseType.OUTBOX, OUTBOX_DB_CLEAR_SCRIPT);

    this.typeScriptMapPostgres.put(ApplicationDatabaseType.KADAI, KADAI_DB_CLEAR_SCRIPT);
    this.typeScriptMapPostgres.put(
        ApplicationDatabaseType.KADAI_ADAPTER, KADAI_ADAPTER_DB_CLEAR_POSTGRES);
    this.typeScriptMapPostgres.put(ApplicationDatabaseType.CAMUNDA, CAMUNDA_DB_CLEAR_SCRIPT);
    this.typeScriptMapPostgres.put(ApplicationDatabaseType.OUTBOX, OUTBOX_DB_CLEAR_POSTGRES);
  }

  /**
   * Clears the db.
   *
   * @param dataSource the datasource
   * @param applicationDatabaseType the type of the application database that is to be cleared
   */
  public void clearDb(DataSource dataSource, ApplicationDatabaseType applicationDatabaseType) {
    try (Connection connection = dataSource.getConnection()) {
      ScriptRunner runner = new ScriptRunner(connection);
      LOGGER.debug(connection.getMetaData().toString());

      runner.setStopOnError(false);
      runner.setLogWriter(logWriter);
      runner.setErrorLogWriter(errorLogWriter);

      String dbProductName = connection.getMetaData().getDatabaseProductName();
      String scriptName = this.typeScriptMap.get(applicationDatabaseType);
      if ("PostgreSQL".equals(dbProductName)) {
        scriptName = this.typeScriptMapPostgres.get(applicationDatabaseType);
      }
      LOGGER.debug("using script {} to clear database", scriptName);
      runner.runScript(new InputStreamReader(this.getClass().getResourceAsStream(scriptName)));

    } catch (Exception e) {
      LOGGER.error("caught Exception {}", e.getMessage(), e);
    }
    LOGGER.debug(outWriter.toString());
    String errorMsg = errorWriter.toString().trim();

    if (!errorMsg.isEmpty() && !errorMsg.contains("SQLCODE=-204, SQLSTATE=42704")) {
      LOGGER.error(errorWriter.toString());
    }
  }

  /** encapsulates the type of the application database. */
  public enum ApplicationDatabaseType {
    KADAI,
    KADAI_ADAPTER,
    CAMUNDA,
    OUTBOX
  }
}
