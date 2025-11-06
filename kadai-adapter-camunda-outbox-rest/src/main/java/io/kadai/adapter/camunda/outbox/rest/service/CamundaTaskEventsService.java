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
import io.kadai.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundException;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.repository.CamundaOutboxSqlProvider;
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
public class CamundaTaskEventsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventsService.class);
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

  public List<CamundaTaskEvent> getEvents(MultivaluedMap<String, String> filterParams)
      throws InvalidArgumentException {

    verifyNoInvalidParameters(filterParams);
    Duration lockDuration = null;
    List<CamundaTaskEvent> camundaTaskEvents;
    if (filterParams.containsKey(LOCK_FOR)) {
      lockDuration =
          Duration.of(Long.parseLong(filterParams.get(LOCK_FOR).get(0)), ChronoUnit.SECONDS);
    }
    if (filterParams.containsKey(TYPE) && filterParams.get(TYPE).contains(CREATE)) {

      camundaTaskEvents = getCreateEvents(lockDuration);

    } else if (filterParams.containsKey(TYPE)
        && filterParams.get(TYPE).contains(DELETE)
        && filterParams.get(TYPE).contains(COMPLETE)) {

      camundaTaskEvents = getCompleteAndDeleteEvents(lockDuration);

    } else if (filterParams.containsKey(RETRIES) && filterParams.get(RETRIES) != null) {

      int remainingRetries = getRetries(filterParams.get(RETRIES));

      camundaTaskEvents = getEventsFilteredByRetries(remainingRetries, lockDuration);
    } else {
      camundaTaskEvents = getAllEvents(lockDuration);
    }
    if (LOGGER.isDebugEnabled()) {

      LOGGER.debug(
          "outbox retrieved {} camundaTaskEvents: {}",
          camundaTaskEvents.size(),
          camundaTaskEvents.stream().map(Object::toString).collect(Collectors.joining(";\n")));
    }
    return camundaTaskEvents;
  }

  public void deleteEvents(String idsAsJsonArray) {
    List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsJsonArray);

    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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

  public List<CamundaTaskEvent> getEventsFilteredByRetries(
      Integer remainingRetries, Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries = new ArrayList<>();
    List<Integer> ids = null;

    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
        camundaTaskEventsFilteredByRetries =
            getCamundaTaskEvents(camundaTaskEventFilteredByRetriesResultSet);
        ids =
            camundaTaskEventsFilteredByRetries.stream()
                .map(CamundaTaskEvent::getId)
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
    return camundaTaskEventsFilteredByRetries;
  }

  public String getEventsCount(int remainingRetries) {
    String eventsCount = "{\"eventsCount\":0}";
    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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

  public CamundaTaskEvent setRemainingRetries(int id, int retriesToSet)
      throws CamundaTaskEventNotFoundException {
    CamundaTaskEvent event = getEvent(id);
    event.setRemainingRetries(retriesToSet);

    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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

  public List<CamundaTaskEvent> setRemainingRetriesForMultipleEvents(
      int retries, int retriesToSet) {
    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries =
        getEventsFilteredByRetries(retries, null);
    camundaTaskEventsFilteredByRetries.forEach(
        camundaTaskEvent -> camundaTaskEvent.setRemainingRetries(retriesToSet));

    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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

    return camundaTaskEventsFilteredByRetries;
  }

  public void deleteFailedEvent(int id) {
    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.deleteAllFailedEvents(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.execute();
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete all failed camunda task events", e);
    }
  }

  public CamundaTaskEvent getEvent(int id) throws CamundaTaskEventNotFoundException {
    CamundaTaskEvent camundaTaskEvent;
    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql = sqlProvider.getEvent(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setInt(1, id);
        ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
        if (completeAndDeleteEventsResultSet.next()) {
          camundaTaskEvent = new CamundaTaskEvent();

          camundaTaskEvent.setId(completeAndDeleteEventsResultSet.getInt(1));
          camundaTaskEvent.setType(completeAndDeleteEventsResultSet.getString(2));
          camundaTaskEvent.setCreated(formatDate(completeAndDeleteEventsResultSet.getTimestamp(3)));
          camundaTaskEvent.setPayload(completeAndDeleteEventsResultSet.getString(4));
          camundaTaskEvent.setRemainingRetries(completeAndDeleteEventsResultSet.getInt(5));
          camundaTaskEvent.setBlockedUntil(completeAndDeleteEventsResultSet.getString(6));
          camundaTaskEvent.setError(completeAndDeleteEventsResultSet.getString(7));
          camundaTaskEvent.setCamundaTaskId(completeAndDeleteEventsResultSet.getString(8));

          return camundaTaskEvent;
        }
      }
    } catch (SQLException e) {
      LOGGER.error("Caughr exception while trying to retrieve camunda task event", e);
    }

    throw new CamundaTaskEventNotFoundException("camunda task event not found");
  }

  public List<CamundaTaskEvent> getAllEvents(Duration lockDuration) {
    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    List<Integer> ids = null;
    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
      String sql =
          lockDuration == null
              ? sqlProvider.getAllEvents(OUTBOX_SCHEMA)
              : sqlProvider.getAllAvailableEvents(OUTBOX_SCHEMA);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        if (lockDuration != null) {
          preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId).collect(Collectors.toList());
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
    return camundaTaskEvents;
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
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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

  private List<CamundaTaskEvent> getCreateEvents(Duration lockDuration) {
    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    List<Integer> ids = null;

    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
        camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId).collect(Collectors.toList());
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
    return camundaTaskEvents;
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

  private List<CamundaTaskEvent> getCamundaTaskEvents(ResultSet createEventsResultSet)
      throws SQLException {
    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    while (createEventsResultSet.next()) {
      CamundaTaskEvent camundaTaskEvent = new CamundaTaskEvent();

      camundaTaskEvent.setId(createEventsResultSet.getInt(1));
      camundaTaskEvent.setType(createEventsResultSet.getString(2));
      camundaTaskEvent.setCreated(formatDate(createEventsResultSet.getTimestamp(3)));
      camundaTaskEvent.setPayload(createEventsResultSet.getString(4));
      camundaTaskEvent.setRemainingRetries(createEventsResultSet.getInt(5));
      camundaTaskEvent.setBlockedUntil(createEventsResultSet.getString(6));
      camundaTaskEvent.setError(createEventsResultSet.getString(7));
      camundaTaskEvent.setCamundaTaskId(createEventsResultSet.getString(8));
      camundaTaskEvent.setSystemEngineIdentifier(createEventsResultSet.getString(9));
      camundaTaskEvent.setLockExpiresAt(formatDate(createEventsResultSet.getTimestamp(10)));

      camundaTaskEvents.add(camundaTaskEvent);
    }

    return camundaTaskEvents;
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

  private List<CamundaTaskEvent> getCompleteAndDeleteEvents(Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    List<Integer> ids = null;
    try (Connection connection = getConnection()) {
      final CamundaOutboxSqlProvider sqlProvider =
          CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
        camundaTaskEvents = getCamundaTaskEvents(completeAndDeleteEventsResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId).collect(Collectors.toList());

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

    return camundaTaskEvents;
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
    synchronized (CamundaTaskEventsService.class) {
      if (dataSource == null) {
        return getDataSourceFromPropertiesFile();
      }
    }
    return dataSource;
  }

  private void unlockEvents(List<Integer> ids, Connection connection) throws SQLException {
    final CamundaOutboxSqlProvider sqlProvider =
        CamundaOutboxSqlProvider.valueOf(connection.getMetaData().getDatabaseProductName());
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
