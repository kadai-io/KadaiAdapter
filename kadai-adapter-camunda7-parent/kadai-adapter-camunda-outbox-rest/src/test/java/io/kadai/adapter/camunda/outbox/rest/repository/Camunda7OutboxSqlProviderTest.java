package io.kadai.adapter.camunda.outbox.rest.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.kadai.common.api.exceptions.UnsupportedDatabaseException;
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
