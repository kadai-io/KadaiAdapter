package io.kadai.adapter.camunda.outbox.rest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.camunda.OutboxRestConfiguration;
import io.kadai.adapter.camunda.outbox.rest.config.OutboxDataSource;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.exception.OutboxServiceUnavailableException;
import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.service.Camunda7TaskEventsService;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class Camunda7TaskEventsServiceLockingTest {

  private static final String OUTBOX_SCHEMA = "kadai_tables";
  private static final int MAX_EVENTS_PER_REQUEST = 25;

  private final Camunda7TaskEventsService service = new Camunda7TaskEventsService();

  @BeforeEach
  void setUp() throws SQLException {
    try (Connection connection = OutboxDataSource.get().getConnection();
        Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.execute("drop schema if exists " + OUTBOX_SCHEMA + " cascade");
      statement.execute("create schema " + OUTBOX_SCHEMA);
      statement.execute(
          "create table "
              + OUTBOX_SCHEMA
              + ".event_store ("
              + "id int generated always as identity primary key, "
              + "type varchar(32) not null, "
              + "created timestamp, "
              + "payload clob, "
              + "remaining_retries int not null, "
              + "blocked_until timestamp not null, "
              + "error varchar(1000), "
              + "camunda_task_id varchar(40), "
              + "system_engine_identifier varchar(128), "
              + "lock_expire timestamp null)");
    }
  }

  @Test
  void should_ReturnEachCreateEventOnlyOnce_When_LockedReadersRunConcurrently() throws Exception {
    int eventCount = MAX_EVENTS_PER_REQUEST * 8;
    insertCreateEvents(eventCount, 3);

    List<Integer> retrievedEventIds = retrieveLockedCreateEventIdsConcurrently(12);

    assertThat(retrievedEventIds).hasSize(eventCount).doesNotHaveDuplicates();
  }

  @Test
  void should_ReturnEventCount_When_EventCountQuerySucceeds() throws SQLException {
    insertEvents("create", 7, 0);

    assertThat(service.countEvents(0)).isEqualTo(7);
    assertThat(service.getEventsCount(0)).isEqualTo("{\"eventsCount\":7}");
  }

  @Test
  void should_ThrowServiceUnavailable_When_EventCountQueryFails() throws SQLException {
    try (Connection connection = OutboxDataSource.get().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("drop table " + OUTBOX_SCHEMA + ".event_store");
    }

    assertThatThrownBy(() -> service.countEvents(0))
        .isInstanceOf(OutboxServiceUnavailableException.class)
        .hasMessage("Unable to retrieve Outbox event count");
  }

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void should_ReportFilteredAcquisitionFailureInsteadOfEmptyQueue(boolean create, boolean lease)
      throws SQLException {
    try (Connection connection = OutboxDataSource.get().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("drop table " + OUTBOX_SCHEMA + ".event_store");
    }

    assertThatThrownBy(() -> service.getEvents(acquisitionParams(create, "A", lease)))
        .isInstanceOf(OutboxServiceUnavailableException.class)
        .hasMessageContaining("Unable to acquire filtered");
  }

  @Test
  void should_ReportUnavailableConnectionForFilteredAcquisition() {
    PooledDataSource dataSource = (PooledDataSource) OutboxDataSource.get();
    final String originalUrl = dataSource.getUrl();
    try {
      dataSource.setUrl("jdbc:invalid:outbox-engine-test");
      for (boolean create : List.of(true, false)) {
        assertThatThrownBy(() -> service.getEvents(acquisitionParams(create, "A", true)))
            .isInstanceOf(OutboxServiceUnavailableException.class)
            .hasMessage("Unable to open Outbox connection")
            .hasCauseInstanceOf(SQLException.class);
      }
    } finally {
      dataSource.setUrl(originalUrl);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void should_ReportFailedLeaseUpdateAndAllowRetry(boolean create) throws Exception {
    insertEvents(create ? "create" : "complete", 3, 3, "A");
    try (Connection connection = OutboxDataSource.get().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "alter table "
              + OUTBOX_SCHEMA
              + ".event_store add constraint reject_lease check (lock_expire is null)");

      assertThatThrownBy(() -> service.getEvents(acquisitionParams(create, "A", true)))
          .isInstanceOf(OutboxServiceUnavailableException.class)
          .hasMessageContaining("Unable to acquire filtered");
      assertThat(countActiveLeases("A")).isZero();

      statement.execute(
          "alter table " + OUTBOX_SCHEMA + ".event_store drop constraint reject_lease");
    }

    assertThat(service.getEvents(acquisitionParams(create, "A", true))).hasSize(3);
    assertThat(countActiveLeases("A")).isEqualTo(3);
  }

  @Test
  void should_ReturnEachEventOnlyOnce_When_LockedReadersRetrieveAllEventsConcurrently()
      throws Exception {
    int eventCountPerType = MAX_EVENTS_PER_REQUEST * 4;
    insertEvents("create", eventCountPerType, 3);
    insertEvents("complete", eventCountPerType, 3);
    insertEvents("delete", eventCountPerType, 3);

    List<Integer> retrievedEventIds =
        retrieveLockedEventIdsConcurrently(12, this::retrieveLockedEventIds);

    assertThat(retrievedEventIds).hasSize(eventCountPerType * 3).doesNotHaveDuplicates();
  }

  @Test
  void should_ReturnEachRetryEventOnlyOnce_When_LockedReadersRetrieveRetryEventsConcurrently()
      throws Exception {
    int eventCount = MAX_EVENTS_PER_REQUEST * 8;
    insertEvents("create", eventCount, 2);
    insertEvents("complete", 3, 1);

    List<Integer> retrievedEventIds =
        retrieveLockedEventIdsConcurrently(12, () -> retrieveLockedRetryEventIds(2));

    assertThat(retrievedEventIds).hasSize(eventCount).doesNotHaveDuplicates();
  }

  @Test
  void should_ReturnEachCompleteAndDeleteEventOnlyOnce_When_LockedReadersRunConcurrently()
      throws Exception {
    int eventCountPerType = MAX_EVENTS_PER_REQUEST * 4;
    insertEvents("create", 3, 3);
    insertEvents("complete", eventCountPerType, 3);
    insertEvents("delete", eventCountPerType, 3);

    List<Integer> retrievedEventIds =
        retrieveLockedEventIdsConcurrently(12, this::retrieveLockedCompleteAndDeleteEventIds);

    assertThat(retrievedEventIds).hasSize(eventCountPerType * 2).doesNotHaveDuplicates();
  }

  @Test
  void should_FilterByRetries_When_LockedRetryQueryIsUsed()
      throws SQLException, InvalidArgumentException {
    insertEvents("create", 3, 2);
    insertEvents("create", 2, 1);

    MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
    requestParams.add("retries", "2");
    requestParams.add("lock-for", "30");

    List<Camunda7TaskEvent> events = service.getEvents(requestParams);

    assertThat(events).hasSize(3);
    assertThat(events).extracting(Camunda7TaskEvent::getRemainingRetries).containsOnly(2);
  }

  @Test
  void should_FilterCreateEventsBeforeLimitingAndLeaseOnlyMatchingEngine() throws Exception {
    insertEvents("create", OutboxRestConfiguration.getOutboxMaxNumberOfEvents() * 2, 3, "engine-A");
    insertEvents("create", 5, 3, "engine-B&+ %");

    List<Camunda7TaskEvent> events =
        service.getEvents(filteredParams("create", "engine-B&+ %", true));

    assertThat(events).hasSize(5);
    assertThat(events)
        .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
        .containsOnly("engine-B&+ %");
    assertThat(countActiveLeases("engine-A")).isZero();
    assertThat(countActiveLeases("engine-B&+ %")).isEqualTo(5);
  }

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void should_ApplyBatchLimitToMatchingEngine(boolean create, boolean lease) throws Exception {
    int maxEvents = OutboxRestConfiguration.getOutboxMaxNumberOfEvents();
    String type = create ? "create" : "delete";
    insertEvents(type, maxEvents * 2, 3, "A");
    insertEvents(type, maxEvents + 5, 3, "B");
    insertEvents(create ? "delete" : "create", 3, 3, "B");

    assertThat(service.getEvents(acquisitionParams(create, "B", lease)))
        .hasSize(maxEvents)
        .allSatisfy(
            event -> {
              assertThat(event.getSystemEngineIdentifier()).isEqualTo("B");
              assertThat(event.getType()).isEqualTo(type);
            });
    assertThat(countActiveLeases("A")).isZero();
    assertThat(countActiveLeases("B")).isEqualTo(lease ? maxEvents : 0);
  }

  @Test
  void should_FilterFinishedEventsWithAndWithoutLease() throws Exception {
    insertEvents("complete", OutboxRestConfiguration.getOutboxMaxNumberOfEvents() * 2, 3, "A");
    insertEvents("delete", 3, 3, "A");
    insertEvents("complete", 3, 3, "B");
    insertEvents("delete", 2, 3, "B");

    MultivaluedMap<String, String> params = filteredParams("complete", "B", false);
    params.add("type", "delete");
    assertThat(service.getEvents(params)).hasSize(5);
    assertThat(countActiveLeases("B")).isZero();

    params.add("lock-for", "30");
    List<Camunda7TaskEvent> leased = service.getEvents(params);
    assertThat(leased).hasSize(5);
    assertThat(leased).extracting(Camunda7TaskEvent::getType).contains("complete", "delete");
    assertThat(leased).extracting(Camunda7TaskEvent::getSystemEngineIdentifier).containsOnly("B");
    assertThat(countActiveLeases("A")).isZero();
    assertThat(countActiveLeases("B")).isEqualTo(5);
  }

  @Test
  void should_PreserveUnfilteredRowsIncludingNullEngine() throws Exception {
    insertEvents("create", 1, 3, null);
    insertEvents("create", 1, 3, "A");
    insertEvents("create", 1, 3, "B");

    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.add("type", "create");
    assertThat(service.getEvents(params))
        .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
        .containsExactlyInAnyOrder(null, "A", "B");
    assertThat(service.getEvents(filteredParams("create", "B", false)))
        .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
        .containsExactly("B");
  }

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void should_MatchEngineIdentifierExactly(boolean create, boolean lease) throws Exception {
    String type = create ? "create" : "complete";
    for (String engine : List.of("engine", "ENGINE", "engine ", " engine", "engine&+ %ä")) {
      insertEvents(type, 1, 3, engine);
    }
    insertEvents(type, 1, 3, null);

    assertThat(service.getEvents(acquisitionParams(create, "engine", lease)))
        .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
        .containsExactly("engine");
  }

  @Test
  void should_ExcludeUnavailableCreateEventsAndIncludeExpiredLeases() throws Exception {
    insertEvents("create", 5, 3, "B");
    insertEvents("create", 1, 3, "A");
    updateCreateEvent("lock_expire", Timestamp.from(Instant.now().plusSeconds(120)), "B", 0);
    updateCreateEvent("lock_expire", Timestamp.from(Instant.now().minusSeconds(120)), "B", 1);
    updateCreateEvent("blocked_until", Timestamp.from(Instant.now().plusSeconds(120)), "B", 2);
    updateCreateEvent("remaining_retries", 0, "B", 3);

    List<Camunda7TaskEvent> events = service.getEvents(filteredParams("create", "B", true));

    assertThat(events).hasSize(2);
    assertThat(events)
        .extracting(Camunda7TaskEvent::getCamundaTaskId)
        .containsExactlyInAnyOrder("task-create-3-1", "task-create-3-4");
    assertThat(countActiveLeases("A")).isZero();
    assertThat(countActiveLeases("B")).isEqualTo(3);
  }

  @Test
  void should_RejectMalformedOrUnsupportedEngineRequests() {
    List<MultivaluedMap<String, String>> invalidRequests = new ArrayList<>();
    for (String invalid : List.of("", "  ", "A\nB")) {
      invalidRequests.add(filteredParams("create", invalid, true));
    }
    MultivaluedMap<String, String> repeated = filteredParams("create", "A", true);
    repeated.add("engine", "B");
    invalidRequests.add(repeated);
    MultivaluedMap<String, String> allEvents = new MultivaluedHashMap<>();
    allEvents.add("engine", "A");
    invalidRequests.add(allEvents);
    MultivaluedMap<String, String> retries = filteredParams("create", "A", true);
    retries.add("retries", "1");
    invalidRequests.add(retries);
    invalidRequests.add(filteredParams("complete", "A", true));
    invalidRequests.add(filteredParams("delete", "A", true));
    invalidRequests.add(filteredParams("unknown", "A", true));
    MultivaluedMap<String, String> repeatedType = filteredParams("create", "A", true);
    repeatedType.add("type", "create");
    invalidRequests.add(repeatedType);
    MultivaluedMap<String, String> mixedTypes = acquisitionParams(false, "A", true);
    mixedTypes.add("type", "create");
    invalidRequests.add(mixedTypes);
    MultivaluedMap<String, String> emptyEngineValues = filteredParams("create", "A", true);
    emptyEngineValues.put("engine", List.of());
    invalidRequests.add(emptyEngineValues);

    for (MultivaluedMap<String, String> params : invalidRequests) {
      assertThatThrownBy(() -> service.getEvents(params))
          .isInstanceOf(InvalidArgumentException.class);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void should_LeaseEachEngineOnlyToItsOwnConcurrentConsumer(boolean create) throws Exception {
    insertEvents(create ? "create" : "complete", 20, 3, "A");
    insertEvents(create ? "create" : "delete", 20, 3, "B");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      final Future<List<Camunda7TaskEvent>> a =
          executor.submit(
              () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return service.getEvents(acquisitionParams(create, "A", true));
              });
      final Future<List<Camunda7TaskEvent>> b =
          executor.submit(
              () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return service.getEvents(acquisitionParams(create, "B", true));
              });
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(a.get(10, TimeUnit.SECONDS))
          .hasSize(20)
          .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
          .containsOnly("A");
      assertThat(b.get(10, TimeUnit.SECONDS))
          .hasSize(20)
          .extracting(Camunda7TaskEvent::getSystemEngineIdentifier)
          .containsOnly("B");
      assertThat(countActiveLeases("A")).isEqualTo(20);
      assertThat(countActiveLeases("B")).isEqualTo(20);
    } finally {
      executor.shutdownNow();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void should_NotDuplicateFilteredEventsBetweenConcurrentConsumers(boolean create)
      throws Exception {
    int eventCount = OutboxRestConfiguration.getOutboxMaxNumberOfEvents() * 4;
    insertEvents(create ? "create" : "complete", eventCount, 3, "A");
    insertEvents(create ? "create" : "delete", 20, 3, "B");

    List<Integer> ids =
        retrieveLockedEventIdsConcurrently(
            8,
            () ->
                service.getEvents(acquisitionParams(create, "A", true)).stream()
                    .map(Camunda7TaskEvent::getId)
                    .toList());

    assertThat(ids).hasSize(eventCount).doesNotHaveDuplicates();
    assertThat(countActiveLeases("A")).isEqualTo(eventCount);
    assertThat(countActiveLeases("B")).isZero();
  }

  private MultivaluedMap<String, String> acquisitionParams(
      boolean create, String engine, boolean lease) {
    MultivaluedMap<String, String> params =
        filteredParams(create ? "create" : "complete", engine, lease);
    if (!create) {
      params.add("type", "delete");
    }
    return params;
  }

  private MultivaluedMap<String, String> filteredParams(String type, String engine, boolean lease) {
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.add("type", type);
    params.add("engine", engine);
    if (lease) {
      params.add("lock-for", "30");
    }
    return params;
  }

  private int countActiveLeases(String engine) throws SQLException {
    String sql =
        "select count(*) from "
            + OUTBOX_SCHEMA
            + ".event_store where system_engine_identifier = ? and lock_expire > current_timestamp";
    try (Connection connection = OutboxDataSource.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, engine);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    }
  }

  private void updateCreateEvent(String column, Object value, String engine, int index)
      throws SQLException {
    String sql =
        "update "
            + OUTBOX_SCHEMA
            + ".event_store set "
            + column
            + " = ? where system_engine_identifier = ? and camunda_task_id = ?";
    try (Connection connection = OutboxDataSource.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, value);
      statement.setString(2, engine);
      statement.setString(3, "task-create-3-" + index);
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private List<Integer> retrieveLockedCreateEventIdsConcurrently(int readers) throws Exception {
    return retrieveLockedEventIdsConcurrently(readers, this::retrieveLockedCreateEventIds);
  }

  private List<Integer> retrieveLockedEventIdsConcurrently(
      int readers, LockedEventRetriever lockedEventRetriever) throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(readers);
    CountDownLatch ready = new CountDownLatch(readers);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<List<Integer>>> futures = new ArrayList<>();

    try {
      for (int i = 0; i < readers; i++) {
        futures.add(
            executorService.submit(
                () -> {
                  ready.countDown();
                  assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                  return lockedEventRetriever.retrieve();
                }));
      }

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      List<Integer> eventIds = new ArrayList<>();
      for (Future<List<Integer>> future : futures) {
        eventIds.addAll(future.get(10, TimeUnit.SECONDS));
      }
      return eventIds;
    } finally {
      executorService.shutdownNow();
    }
  }

  private List<Integer> retrieveLockedCreateEventIds() throws InvalidArgumentException {
    MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
    requestParams.add("type", "create");
    requestParams.add("lock-for", "30");

    return service.getEvents(requestParams).stream().map(Camunda7TaskEvent::getId).toList();
  }

  private List<Integer> retrieveLockedEventIds() throws InvalidArgumentException {
    MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
    requestParams.add("lock-for", "30");

    return service.getEvents(requestParams).stream().map(Camunda7TaskEvent::getId).toList();
  }

  private List<Integer> retrieveLockedRetryEventIds(int remainingRetries)
      throws InvalidArgumentException {
    MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
    requestParams.add("retries", String.valueOf(remainingRetries));
    requestParams.add("lock-for", "30");

    return service.getEvents(requestParams).stream().map(Camunda7TaskEvent::getId).toList();
  }

  private List<Integer> retrieveLockedCompleteAndDeleteEventIds() throws InvalidArgumentException {
    MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
    requestParams.add("type", "complete");
    requestParams.add("type", "delete");
    requestParams.add("lock-for", "30");

    return service.getEvents(requestParams).stream().map(Camunda7TaskEvent::getId).toList();
  }

  private void insertCreateEvents(int eventCount, int remainingRetries) throws SQLException {
    insertEvents("create", eventCount, remainingRetries);
  }

  private void insertEvents(String type, int eventCount, int remainingRetries) throws SQLException {
    insertEvents(type, eventCount, remainingRetries, null);
  }

  private void insertEvents(String type, int eventCount, int remainingRetries, String engine)
      throws SQLException {
    String sql =
        "insert into "
            + OUTBOX_SCHEMA
            + ".event_store "
            + "(type, created, payload, remaining_retries, blocked_until, camunda_task_id,"
            + " system_engine_identifier) values (?, ?, ?, ?, ?, ?, ?)";

    try (Connection connection = OutboxDataSource.get().getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      connection.setAutoCommit(true);
      Timestamp availableSince = Timestamp.from(Instant.now().minusSeconds(5));
      for (int i = 0; i < eventCount; i++) {
        preparedStatement.setString(1, type);
        preparedStatement.setTimestamp(2, availableSince);
        preparedStatement.setString(3, "{}");
        preparedStatement.setInt(4, remainingRetries);
        preparedStatement.setTimestamp(5, availableSince);
        preparedStatement.setString(6, "task-" + type + "-" + remainingRetries + "-" + i);
        preparedStatement.setString(7, engine);
        preparedStatement.addBatch();
      }
      preparedStatement.executeBatch();
    }
  }

  @FunctionalInterface
  private interface LockedEventRetriever {
    List<Integer> retrieve() throws InvalidArgumentException;
  }
}
