package io.kadai.adapter.camunda.outbox.rest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxDataSource;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.service.Camunda7TaskEventsService;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    String sql =
        "insert into "
            + OUTBOX_SCHEMA
            + ".event_store "
            + "(type, created, payload, remaining_retries, blocked_until, camunda_task_id) "
            + "values (?, ?, ?, ?, ?, ?)";

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
