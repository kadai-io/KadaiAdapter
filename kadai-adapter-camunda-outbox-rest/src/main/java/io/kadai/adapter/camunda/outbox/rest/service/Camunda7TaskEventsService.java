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

package io.kadai.adapter.camunda.outbox.rest.service;

import io.kadai.adapter.camunda.OutboxRestConfiguration;
import io.kadai.adapter.camunda.outbox.rest.exception.Camunda7TaskEventNotFoundException;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.repository.Camunda7OutboxSqlProvider;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

/** Implementation of the Outbox REST service. */
public class Camunda7TaskEventsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(Camunda7TaskEventsService.class);
  private static final String CREATE = "create";
  private static final String COMPLETE = "complete";
  private static final String DELETE = "delete";
  private static final String RETRIES = "retries";
  private static final String TYPE = "type";
  private static final String LOCK_FOR = "lock-for";
  private static final List<String> ALLOWED_PARAMS =
      Stream.of(TYPE, RETRIES, LOCK_FOR).collect(Collectors.toList());
  private static final String OUTBOX_SCHEMA = OutboxRestConfiguration.getOutboxSchema();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static int maxNumberOfEventsReturned = 0;

  static {
    maxNumberOfEventsReturned = OutboxRestConfiguration.getOutboxMaxNumberOfEvents();
    LOGGER.info(
        "Outbox Rest Api will return at max {} events per request", maxNumberOfEventsReturned);
  }

  private DataSource dataSource = null;

  public List<Camunda7TaskEvent> getEvents(MultivaluedMap<String, String> filterParams)
      throws InvalidArgumentException {

    verifyNoInvalidParameters(filterParams);
    Duration lockDuration = null;
    List<Camunda7TaskEvent> camunda7TaskEvents;
    if (filterParams.containsKey(LOCK_FOR)) {
      lockDuration =
          Duration.of(Long.parseLong(filterParams.get(LOCK_FOR).get(0)), ChronoUnit.SECONDS);
    }
    if (filterParams.containsKey(TYPE) && filterParams.get(TYPE).contains(CREATE)) {

      camunda7TaskEvents = getCreateEvents(lockDuration);

    } else if (filterParams.containsKey(TYPE)
        && filterParams.get(TYPE).contains(DELETE)
        && filterParams.get(TYPE).contains(COMPLETE)) {

      camunda7TaskEvents = getCompleteAndDeleteEvents(lockDuration);

    } else if (filterParams.containsKey(RETRIES) && filterParams.get(RETRIES) != null) {

      int remainingRetries = getRetries(filterParams.get(RETRIES));

      camunda7TaskEvents = getEventsFilteredByRetries(remainingRetries, lockDuration);
    } else {
      camunda7TaskEvents = getAllEvents(lockDuration);
    }
    if (LOGGER.isDebugEnabled()) {

      LOGGER.debug(
          "outbox retrieved {} camunda7TaskEvents: {}",
          camunda7TaskEvents.size(),
          camunda7TaskEvents.stream().map(Object::toString).collect(Collectors.joining(";\n")));
    }
    return camunda7TaskEvents;
  }

  public void deleteEvents(String idsAsJsonArray) {
    List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsJsonArray);

    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          sqlProvider.getSqlWithoutPlaceholdersDeleteEvents(
              OUTBOX_SCHEMA, preparePlaceHolders(idsAsIntegers.size()));

      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        setPreparedStatementValues(preparedStatement, idsAsIntegers);
        preparedStatement.execute();
      }

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete events from the outbox table", e);
    }
  }

  public void decreaseRemainingRetriesAndLogError(String eventIdAndErrorLog) {
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.decreaseRemainingRetries(OUTBOX_SCHEMA);

      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        JsonNode id = OBJECT_MAPPER.readTree(eventIdAndErrorLog).get("taskEventId");
        JsonNode errorLog = OBJECT_MAPPER.readTree(eventIdAndErrorLog).get("errorLog");

        Instant blockedUntil = getBlockedUntil();
        preparedStatement.setTimestamp(1, Timestamp.from(blockedUntil));
        preparedStatement.setString(2, errorLog.toString());
        preparedStatement.setInt(3, id.asInt());
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to decrease the remaining retries of camunda task event",
          e);
    }
  }

  public List<Camunda7TaskEvent> getEventsFilteredByRetries(
      Integer remainingRetries, Duration lockDuration) {

    List<Camunda7TaskEvent> camunda7TaskEventsFilteredByRetries = new ArrayList<>();
    List<Integer> ids = null;

    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          lockDuration == null
              ? sqlProvider.getEventsFilteredByRetries(OUTBOX_SCHEMA)
              : sqlProvider.getAvailableEventsFilteredByRetries(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, remainingRetries);
        if (lockDuration != null) {
          preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventFilteredByRetriesResultSet = preparedStatement.executeQuery();
        camunda7TaskEventsFilteredByRetries =
            getCamundaTaskEvents(camundaTaskEventFilteredByRetriesResultSet);
        ids =
            camunda7TaskEventsFilteredByRetries.stream()
                .map(Camunda7TaskEvent::getId)
                .collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve failed events from the outbox", e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve failed events from the outbox", e);
    }
    return camunda7TaskEventsFilteredByRetries;
  }

  public String getEventsCount(int remainingRetries) {
    String eventsCount = "{\"eventsCount\":0}";
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.getEventsCount(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, remainingRetries);
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        if (camundaTaskEventResultSet.next()) {
          eventsCount =
              eventsCount.replace("0", String.valueOf(camundaTaskEventResultSet.getInt(1)));
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve events count from the outbox", e);
    }
    return eventsCount;
  }

  public Camunda7TaskEvent setRemainingRetries(int id, int retriesToSet)
      throws Camunda7TaskEventNotFoundException {
    Camunda7TaskEvent event = getEvent(id);
    event.setRemainingRetries(retriesToSet);

    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.setRemainingRetries(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, retriesToSet);
        preparedStatement.setInt(2, id);
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to set remaining retries for camunda task event", e);
    }

    return event;
  }

  public List<Camunda7TaskEvent> setRemainingRetriesForMultipleEvents(
      int retries, int retriesToSet) {
    List<Camunda7TaskEvent> camunda7TaskEventsFilteredByRetries =
        getEventsFilteredByRetries(retries, null);
    camunda7TaskEventsFilteredByRetries.forEach(
        camundaTaskEvent -> camundaTaskEvent.setRemainingRetries(retriesToSet));

    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.setRemainingRetriesForMultipleEvents(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, retriesToSet);
        preparedStatement.setInt(2, retries);
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to set remaining retries "
              + "for all filtered by retries camunda task events",
          e);
    }

    return camunda7TaskEventsFilteredByRetries;
  }

  public void deleteFailedEvent(int id) {
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.deleteFailedEvent(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, id);
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete failed camunda task event", e);
    }
  }

  public void deleteAllFailedEvents() {
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.deleteAllFailedEvents(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete all failed camunda task events", e);
    }
  }

  public Camunda7TaskEvent getEvent(int id) throws Camunda7TaskEventNotFoundException {
    Camunda7TaskEvent camunda7TaskEvent;
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.getEvent(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, id);
        ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
        if (completeAndDeleteEventsResultSet.next()) {
          camunda7TaskEvent = new Camunda7TaskEvent();

          camunda7TaskEvent.setId(completeAndDeleteEventsResultSet.getInt(1));
          camunda7TaskEvent.setType(completeAndDeleteEventsResultSet.getString(2));
          camunda7TaskEvent.setCreated(formatDate(completeAndDeleteEventsResultSet.getTimestamp(3)));
          camunda7TaskEvent.setPayload(completeAndDeleteEventsResultSet.getString(4));
          camunda7TaskEvent.setRemainingRetries(completeAndDeleteEventsResultSet.getInt(5));
          camunda7TaskEvent.setBlockedUntil(completeAndDeleteEventsResultSet.getString(6));
          camunda7TaskEvent.setError(completeAndDeleteEventsResultSet.getString(7));
          camunda7TaskEvent.setCamundaTaskId(completeAndDeleteEventsResultSet.getString(8));

          return camunda7TaskEvent;
        }
      }
    } catch (SQLException e) {
      LOGGER.error("Caughr exception while trying to retrieve camunda task event", e);
    }

    throw new Camunda7TaskEventNotFoundException("camunda task event not found");
  }

  public List<Camunda7TaskEvent> getAllEvents(Duration lockDuration) {
    List<Camunda7TaskEvent> camunda7TaskEvents = new ArrayList<>();
    List<Integer> ids = null;
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          lockDuration == null
              ? sqlProvider.getAllEvents(OUTBOX_SCHEMA)
              : sqlProvider.getAllAvailableEvents(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        if (lockDuration != null) {
          preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camunda7TaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camunda7TaskEvents.stream().map(Camunda7TaskEvent::getId).collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
    }
    return camunda7TaskEvents;
  }

  public void unlockEventForId(Integer eventId) {
    try (Connection connection = getConnection()) {
      unlockEvents(Collections.singletonList((eventId)), connection);
    } catch (Exception e) {
      LOGGER.error("Failed to unlock events", e);
    }
  }

  public void lockEvents(List<Integer> ids, Duration lockDuration, Connection connection) {
    if (lockDuration == null || ids.isEmpty()) {
      return;
    }
    String commaSeparatedIds = ids.stream().map(Object::toString).collect(Collectors.joining(", "));
    try {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.setLockExpire(OUTBOX_SCHEMA, commaSeparatedIds);

      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setTimestamp(1, Timestamp.from(Instant.now().plus(lockDuration)));
        preparedStatement.execute();
      }
    } catch (SQLException e) {
      LOGGER.error("Caught exception while trying to lock events", e);
    }
  }

  private int getRetries(List<String> retries) throws InvalidArgumentException {
    try {
      return Integer.parseInt(retries.get(0));
    } catch (NumberFormatException e) {
      throw new InvalidArgumentException("retries param must be of type Integer!");
    }
  }

  private void verifyNoInvalidParameters(MultivaluedMap<String, String> filterParams)
      throws InvalidArgumentException {
    List<String> invalidParams =
        filterParams.keySet().stream()
            .filter(key -> !ALLOWED_PARAMS.contains(key))
            .collect(Collectors.toList());

    if (!invalidParams.isEmpty()) {
      throw new InvalidArgumentException("Provided invalid request params: " + invalidParams);
    }
  }

  private Instant getBlockedUntil() {
    Duration blockedDuration = OutboxRestConfiguration.getDurationBetweenTaskCreationRetries();
    return Instant.now().plus(blockedDuration);
  }

  private static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }

  private List<Camunda7TaskEvent> getCreateEvents(Duration lockDuration) {
    List<Camunda7TaskEvent> camunda7TaskEvents = new ArrayList<>();
    List<Integer> ids = null;

    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          lockDuration == null
              ? sqlProvider.getCreateEvents(OUTBOX_SCHEMA, maxNumberOfEventsReturned)
              : sqlProvider.getAvailableCreateEvents(OUTBOX_SCHEMA, maxNumberOfEventsReturned);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, CREATE);
        preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));
        if (lockDuration != null) {
          preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camunda7TaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camunda7TaskEvents.stream().map(Camunda7TaskEvent::getId).collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (SQLException | NullPointerException e) {
      LOGGER.warn("Caught Exception while trying to retrieve create events from the outbox", e);
    }
    return camunda7TaskEvents;
  }

  private String preparePlaceHolders(int length) {
    return String.join(",", Collections.nCopies(length, "?"));
  }

  private void setPreparedStatementValues(PreparedStatement preparedStatement, List<Integer> ids)
      throws SQLException {
    for (int i = 0; i < ids.size(); i++) {
      preparedStatement.setObject(i + 1, ids.get(i));
    }
  }

  private List<Camunda7TaskEvent> getCamundaTaskEvents(ResultSet createEventsResultSet)
      throws SQLException {
    List<Camunda7TaskEvent> camunda7TaskEvents = new ArrayList<>();

    while (createEventsResultSet.next()) {
      Camunda7TaskEvent camunda7TaskEvent = new Camunda7TaskEvent();

      camunda7TaskEvent.setId(createEventsResultSet.getInt(1));
      camunda7TaskEvent.setType(createEventsResultSet.getString(2));
      camunda7TaskEvent.setCreated(formatDate(createEventsResultSet.getTimestamp(3)));
      camunda7TaskEvent.setPayload(createEventsResultSet.getString(4));
      camunda7TaskEvent.setRemainingRetries(createEventsResultSet.getInt(5));
      camunda7TaskEvent.setBlockedUntil(createEventsResultSet.getString(6));
      camunda7TaskEvent.setError(createEventsResultSet.getString(7));
      camunda7TaskEvent.setCamundaTaskId(createEventsResultSet.getString(8));
      camunda7TaskEvent.setSystemEngineIdentifier(createEventsResultSet.getString(9));
      camunda7TaskEvent.setLockExpiresAt(formatDate(createEventsResultSet.getTimestamp(10)));

      camunda7TaskEvents.add(camunda7TaskEvent);
    }

    return camunda7TaskEvents;
  }

  private List<Integer> getIdsAsIntegers(String idsAsJsonArray) {
    ObjectMapper objectMapper = new ObjectMapper();
    List<Integer> idsAsIntegers = new ArrayList<>();

    try {
      JsonNode idsAsJsonArrayNode = objectMapper.readTree(idsAsJsonArray).get("taskCreationIds");
      if (idsAsJsonArrayNode != null) {
        idsAsJsonArrayNode.forEach(id -> idsAsIntegers.add(id.asInt()));
      }
    } catch (IOException e) {
      LOGGER.warn(
          "Caught IOException while trying to read the passed JSON-Object in the POST-Request"
              + " to delete events from the outbox table",
          e);
    }
    return idsAsIntegers;
  }

  private List<Camunda7TaskEvent> getCompleteAndDeleteEvents(Duration lockDuration) {

    List<Camunda7TaskEvent> camunda7TaskEvents = new ArrayList<>();
    List<Integer> ids = null;
    try (Connection connection = getConnection()) {
      final Camunda7OutboxSqlProvider sqlProvider =
          Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          lockDuration == null
              ? sqlProvider.getCompleteAndDeleteEvents(OUTBOX_SCHEMA, maxNumberOfEventsReturned)
              : sqlProvider.getAvailableCompleteAndDeleteEvents(
                  OUTBOX_SCHEMA, maxNumberOfEventsReturned);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, COMPLETE);
        preparedStatement.setString(2, DELETE);
        if (lockDuration != null) {
          preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
        }
        ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
        camunda7TaskEvents = getCamundaTaskEvents(completeAndDeleteEventsResultSet);
        ids = camunda7TaskEvents.stream().map(Camunda7TaskEvent::getId).collect(Collectors.toList());

        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve complete/delete events from the outbox", e);
    }

    return camunda7TaskEvents;
  }

  private Connection getConnection() {

    Connection connection = null;
    try {
      connection = getDataSource().getConnection();
    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve a connection from the provided datasource",
          e.getClass().getName());
    }

    if (connection == null) {
      LOGGER.warn("Retrieved connection was NULL, Please make sure to provide a valid datasource.");
      throw new RuntimeException(
          "Retrieved connection was NULL. Please make sure to provide a valid datasource.");
    }
    return connection;
  }

  private DataSource getDataSource() {
    synchronized (Camunda7TaskEventsService.class) {
      if (dataSource == null) {
        return getDataSourceFromPropertiesFile();
      }
    }
    return dataSource;
  }

  private void unlockEvents(List<Integer> ids, Connection connection) throws SQLException {
    final Camunda7OutboxSqlProvider sqlProvider =
        Camunda7OutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
    String commaSeparatedIds = ids.stream().map(Object::toString).collect(Collectors.joining(", "));
    String sql = sqlProvider.unlock(OUTBOX_SCHEMA, commaSeparatedIds);
    try (PreparedStatement preparedStatement2 = connection.prepareStatement(sql)) {
      preparedStatement2.execute();
    }
  }

  private DataSource getDataSourceFromPropertiesFile() {
    try {

      String jndiUrl = OutboxRestConfiguration.getOutboxDatasourceJndi();
      if (jndiUrl != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiUrl);

      } else {

        dataSource =
            createDatasource(
                OutboxRestConfiguration.getOutboxDatasourceDriver(),
                OutboxRestConfiguration.getOutboxDatasourceUrl(),
                OutboxRestConfiguration.getOutboxDatasourceUsername(),
                OutboxRestConfiguration.getOutboxDatasourcePassword());
      }

    } catch (NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }

    return dataSource;
  }

  private String formatDate(Date date) {
    if (date == null) {
      return null;
    } else {
      return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
          .withZone(ZoneId.systemDefault())
          .format(date.toInstant());
    }
  }
}
