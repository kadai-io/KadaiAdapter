package io.kadai.adapter.test;

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
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class KadaiAdapterSpringBootTestExecutionListener implements TestExecutionListener {

  private static DataSource kadaiDataSource;

  @Override
  public void beforeTestClass(@NonNull TestContext testContext) throws Exception {
    ApplicationContext context = testContext.getApplicationContext();
    kadaiDataSource = context.getBean("kadaiDataSource", DataSource.class);
  }

  @Override
  public void beforeTestMethod(@NonNull TestContext testContext) throws Exception {
    DbCleaner cleaner = new DbCleaner();
    cleaner.clearDb(kadaiDataSource, DbCleaner.ApplicationDatabaseType.KADAI);
  }

  /** Helper class to clean up databases. */
  static class DbCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbCleaner.class);
    private static final String KADAI_DB_CLEAR_SCRIPT = "/sql/clear-kadai-db.sql";
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
      this.typeScriptMapPostgres.put(ApplicationDatabaseType.KADAI, KADAI_DB_CLEAR_SCRIPT);
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
    }
  }
}
