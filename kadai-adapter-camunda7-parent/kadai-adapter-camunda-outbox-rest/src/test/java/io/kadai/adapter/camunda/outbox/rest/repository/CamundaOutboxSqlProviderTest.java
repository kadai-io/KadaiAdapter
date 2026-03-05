package io.kadai.adapter.camunda.outbox.rest.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.kadai.common.api.exceptions.UnsupportedDatabaseException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CamundaOutboxSqlProviderTest {

  @ParameterizedTest
  @MethodSource("supportedDatabases")
  void shouldReturnCorrectProvider(
      String databaseProductName, Class<? extends CamundaOutboxSqlProvider> expectedClass) {

    CamundaOutboxSqlProvider provider = CamundaOutboxSqlProvider.valueOf(databaseProductName);

    assertNotNull(provider);
    assertEquals(expectedClass, provider.getClass());
  }

  static Stream<String> unsupportedDatabases() {
    return Stream.of("MySQL", "MariaDB", "SQLServer", "SQLite", "unknown-db");
  }

  @ParameterizedTest
  @MethodSource("unsupportedDatabases")
  void shouldThrowForUnsupportedDatabase(String databaseProductName) {
    assertThrows(
        UnsupportedDatabaseException.class,
        () -> CamundaOutboxSqlProvider.valueOf(databaseProductName));
  }

  static Stream<Arguments> supportedDatabases() {
    return Stream.of(
        Arguments.of("H2", H2CamundaOutboxSqlProvider.class),
        Arguments.of("h2", H2CamundaOutboxSqlProvider.class),
        Arguments.of("DB2", Db2CamundaOutboxSqlProvider.class),
        Arguments.of("db2", Db2CamundaOutboxSqlProvider.class),
        Arguments.of("IBM DB2", Db2CamundaOutboxSqlProvider.class),
        Arguments.of("DB2/LINUXX8664", Db2CamundaOutboxSqlProvider.class),
        Arguments.of("PostgreSQL", PostgresCamundaOutboxSqlProvider.class),
        Arguments.of("postgresql", PostgresCamundaOutboxSqlProvider.class),
        Arguments.of("Oracle", OracleCamundaOutboxSqlProvider.class),
        Arguments.of("oracle database", OracleCamundaOutboxSqlProvider.class));
  }
}
