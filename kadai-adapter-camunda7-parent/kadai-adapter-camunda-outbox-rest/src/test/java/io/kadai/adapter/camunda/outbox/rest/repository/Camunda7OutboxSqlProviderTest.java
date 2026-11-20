package io.kadai.adapter.camunda.outbox.rest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.kadai.common.api.exceptions.UnsupportedDatabaseException;
import java.util.Locale;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Camunda7OutboxSqlProviderTest {

  @ParameterizedTest
  @MethodSource("supportedDatabases")
  void should_ReturnCorrectProvider(
      String databaseProductName, Class<? extends Camunda7OutboxSqlProvider> expectedClass) {

    Camunda7OutboxSqlProvider provider = Camunda7OutboxSqlProvider.valueOf(databaseProductName);

    assertNotNull(provider);
    assertEquals(expectedClass, provider.getClass());
  }

  @ParameterizedTest
  @MethodSource("unsupportedDatabases")
  void should_Throw_ForUnsupportedDatabase(String databaseProductName) {
    final ThrowingCallable call = () -> Camunda7OutboxSqlProvider.valueOf(databaseProductName);
    Assertions.assertThatExceptionOfType(UnsupportedDatabaseException.class)
        .isThrownBy(call)
        .extracting(UnsupportedDatabaseException::getDatabaseProductName)
        .isEqualTo(databaseProductName);
  }

  @ParameterizedTest
  @MethodSource("acquisitionQueries")
  void should_FilterBeforeLimitingAndLockingWithEngineBoundLast(
      String database, boolean create, boolean lease) {
    Camunda7OutboxSqlProvider provider = Camunda7OutboxSqlProvider.valueOf(database);
    String filtered;
    String unfiltered;
    if (create) {
      filtered =
          lease
              ? provider.getAvailableCreateEventsFilteredBySystemEngineId("kadai_tables", 7)
              : provider.getCreateEventsFilteredBySystemEngineId("kadai_tables", 7);
      unfiltered =
          lease
              ? provider.getAvailableCreateEvents("kadai_tables", 7)
              : provider.getCreateEvents("kadai_tables", 7);
    } else {
      filtered =
          lease
              ? provider.getAvailableCompleteAndDeleteEventsFilteredBySystemEngineId(
                  "kadai_tables", 7)
              : provider.getCompleteAndDeleteEventsFilteredBySystemEngineId("kadai_tables", 7);
      unfiltered =
          lease
              ? provider.getAvailableCompleteAndDeleteEvents("kadai_tables", 7)
              : provider.getCompleteAndDeleteEvents("kadai_tables", 7);
    }
    String sql = filtered.toLowerCase(Locale.ROOT);
    int enginePosition = sql.indexOf("system_engine_identifier");
    int limitPosition = sql.indexOf("fetch first 7 rows only");

    assertThat(enginePosition).isGreaterThan(sql.indexOf("where"));
    assertThat(limitPosition).isGreaterThan(enginePosition);
    assertThat(sql.substring(0, enginePosition).chars().filter(c -> c == '?').count())
        .isEqualTo(lease ? 3 : 2);
    assertThat(sql.substring(enginePosition).chars().filter(c -> c == '?').count()).isEqualTo(1);
    assertThat(unfiltered).doesNotContain("system_engine_identifier");
    assertThat(unfiltered.chars().filter(c -> c == '?').count()).isEqualTo(lease ? 3 : 2);

    if (create) {
      assertThat(sql).contains("remaining_retries>0", "blocked_until < ?");
    } else {
      assertThat(sql).contains("where (type = ? or type = ?)");
    }
    if (lease) {
      assertThat(sql).contains("(lock_expire < ? or lock_expire is null)");
      assertThat(sql)
          .endsWith(
              database.equals("DB2")
                  ? "for read only with rs use and keep update locks"
                  : "for update skip locked");
      if (database.equals("Oracle")) {
        assertThat(sql)
            .contains("where rowid in (select rowid from kadai_tables.event_store where");
        assertThat(sql.indexOf("select rowid")).isLessThan(enginePosition);
        assertThat(sql.indexOf(") for update")).isGreaterThan(limitPosition);
      }
    }
    if (database.equals("DB2")) {
      assertThat(sql)
          .contains("varbinary(system_engine_identifier) = varbinary(cast(? as varchar(");
    } else if (database.equals("H2")) {
      assertThat(sql)
          .contains("cast(system_engine_identifier as varbinary) = cast(? as varbinary)");
    }
  }

  static Stream<Arguments> acquisitionQueries() {
    return Stream.of("H2", "PostgreSQL", "DB2", "Oracle")
        .flatMap(
            database ->
                Stream.of(true, false)
                    .flatMap(
                        create ->
                            Stream.of(true, false)
                                .map(lease -> Arguments.of(database, create, lease))));
  }

  static Stream<Arguments> supportedDatabases() {
    return Stream.of(
        Arguments.of("H2", H2Camunda7OutboxSqlProvider.class),
        Arguments.of("h2", H2Camunda7OutboxSqlProvider.class),
        Arguments.of("DB2", Db2Camunda7OutboxSqlProvider.class),
        Arguments.of("db2", Db2Camunda7OutboxSqlProvider.class),
        Arguments.of("IBM DB2", Db2Camunda7OutboxSqlProvider.class),
        Arguments.of("DB2/LINUXX8664", Db2Camunda7OutboxSqlProvider.class),
        Arguments.of("PostgreSQL", PostgresCamunda7OutboxSqlProvider.class),
        Arguments.of("postgresql", PostgresCamunda7OutboxSqlProvider.class),
        Arguments.of("Oracle", OracleCamunda7OutboxSqlProvider.class),
        Arguments.of("oracle database", OracleCamunda7OutboxSqlProvider.class));
  }

  static Stream<String> unsupportedDatabases() {
    return Stream.of("MySQL", "MariaDB", "SQLServer", "SQLite", "unknown-db");
  }
}
