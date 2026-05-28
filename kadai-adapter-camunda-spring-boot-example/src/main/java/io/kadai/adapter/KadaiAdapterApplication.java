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

package io.kadai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.KadaiConfiguration;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.classification.api.ClassificationService;
import io.kadai.classification.api.exceptions.ClassificationNotFoundException;
import io.kadai.classification.api.models.Classification;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.api.KadaiEngine.ConnectionManagementMode;
import io.kadai.common.internal.SpringKadaiEngine;
import io.kadai.workbasket.api.WorkbasketPermission;
import io.kadai.workbasket.api.WorkbasketService;
import io.kadai.workbasket.api.WorkbasketType;
import io.kadai.workbasket.api.exceptions.WorkbasketNotFoundException;
import io.kadai.workbasket.api.models.Workbasket;
import io.kadai.workbasket.api.models.WorkbasketAccessItem;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Application that provides an adapter between KADAI and one or more external systems. */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class KadaiAdapterApplication implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiAdapterApplication.class);
  private static final DateTimeFormatter OUTBOX_PAYLOAD_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  private static final String SEED_CAMUNDA_OUTBOX_ARGUMENT = "seed-camunda-outbox";
  private static final String CREATE_EVENT_TYPE = "create";
  private static final String OUTBOX_SCHEMA = "kadai_tables";
  private static final String OUTBOX_SCHEMA_VERSION = "1.12.0";
  private static final int MANUAL_OUTBOX_EVENT_COUNT = 10000;
  private static final int INITIAL_REMAINING_RETRIES = 5;
  private static final String WORKBASKET_OWNER = "taskadmin";

  private final ObjectMapper objectMapper;
  private final DataSource kadaiDataSource;
  private final SpringKadaiEngine kadaiEngine;

  @Value("${manual.camunda.outbox.seed.jdbc.driver:org.postgresql.Driver}")
  private String outboxJdbcDriver;

  @Value("${manual.camunda.outbox.seed.jdbc.url:jdbc:postgresql://localhost:55432/postgres}")
  private String outboxJdbcUrl;

  @Value("${manual.camunda.outbox.seed.jdbc.username:postgres}")
  private String outboxJdbcUsername;

  @Value("${manual.camunda.outbox.seed.jdbc.password:postgres}")
  private String outboxJdbcPassword;

  @Value("${manual.camunda.outbox.seed.system-url:http://localhost:8081/example-context-root/engine-rest}")
  private String camundaSystemUrl;

  @Value("${manual.camunda.outbox.seed.system-engine-identifier:}")
  private String systemEngineIdentifier;

  @Value("${manual.camunda.outbox.seed.domain:DOMAIN_A}")
  private String defaultDomain;

  @Value("${manual.camunda.outbox.seed.classification-key:T6310}")
  private String defaultClassificationKey;

  @Value("${manual.camunda.outbox.seed.workbasket-key:GPK_KSC}")
  private String defaultWorkbasketKey;

  @Value("${manual.camunda.outbox.seed.task-definition-key:manual-local-ingestion}")
  private String defaultTaskDefinitionKey;

  @Value("${manual.camunda.outbox.seed.enabled:false}")
  private boolean manualCamundaOutboxSeedEnabled;

  @Value("${kadai.schemaName:KADAI}")
  private String kadaiSchemaName;

  public KadaiAdapterApplication(
      ObjectMapper objectMapper,
      @Qualifier("kadaiDataSource") DataSource kadaiDataSource,
      SpringKadaiEngine kadaiEngine) {
    this.objectMapper = objectMapper;
    this.kadaiDataSource = kadaiDataSource;
    this.kadaiEngine = kadaiEngine;
  }

  public static void main(String[] args) {
    SpringApplication.run(KadaiAdapterApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!manualCamundaOutboxSeedEnabled && !args.containsOption(SEED_CAMUNDA_OUTBOX_ARGUMENT)) {
      return;
    }

    if (MANUAL_OUTBOX_EVENT_COUNT <= 0) {
      LOGGER.info(
          "Skipping manual Camunda outbox seeding because MANUAL_OUTBOX_EVENT_COUNT is {}.",
          MANUAL_OUTBOX_EVENT_COUNT);
      return;
    }

    LOGGER.info(
        "Manual Camunda outbox seeding is enabled. The datasource properties under "
            + "manual.camunda.outbox.seed.* must point to the same database that backs your "
            + "Camunda outbox REST service.");

    kadaiEngine.runAsAdmin(
        () -> {
          try {
            ensureKadaiInfrastructure();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    Class.forName(outboxJdbcDriver);

    try (Connection connection =
        DriverManager.getConnection(
            outboxJdbcUrl, outboxJdbcUsername, outboxJdbcPassword)) {
      boolean initialAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);

      try {
        ensureOutboxSchema(connection);
        List<Long> insertedEventIds = insertManualCreateEvents(connection);
        connection.commit();

        LOGGER.info(
            "Inserted {} manual Camunda outbox create events into {}.EVENT_STORE. Event IDs: {}",
            insertedEventIds.size(),
            OUTBOX_SCHEMA,
            insertedEventIds);
      } catch (Exception e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(initialAutoCommit);
      }
    }
  }

  private void ensureKadaiInfrastructure() throws Exception {
    KadaiConfiguration kadaiConfiguration =
        new KadaiConfiguration.Builder(kadaiDataSource, false, kadaiSchemaName)
            .initKadaiProperties()
            .build();

    KadaiEngine unsecuredKadaiEngine =
        KadaiEngine.buildKadaiEngine(kadaiConfiguration, ConnectionManagementMode.AUTOCOMMIT);

    createWorkbasketIfMissing(unsecuredKadaiEngine, defaultWorkbasketKey, defaultDomain);
    createClassificationIfMissing(unsecuredKadaiEngine, defaultClassificationKey, defaultDomain);
  }

  private void createWorkbasketIfMissing(
      KadaiEngine kadaiEngine, String workbasketKey, String domain) throws Exception {
    WorkbasketService workbasketService = kadaiEngine.getWorkbasketService();

    try {
      workbasketService.getWorkbasket(workbasketKey, domain);
    } catch (WorkbasketNotFoundException e) {
      Workbasket workbasket = workbasketService.newWorkbasket(workbasketKey, domain);
      workbasket.setName(workbasketKey);
      workbasket.setOwner(WORKBASKET_OWNER);
      workbasket.setType(WorkbasketType.PERSONAL);
      workbasket = workbasketService.createWorkbasket(workbasket);
      createWorkbasketAccessItem(workbasketService, workbasket);
      LOGGER.info(
          "Created workbasket {} in domain {} for manual local testing.",
          workbasketKey,
          domain);
    }
  }

  private void createWorkbasketAccessItem(
      WorkbasketService workbasketService, Workbasket workbasket) throws Exception {
    WorkbasketAccessItem workbasketAccessItem =
        workbasketService.newWorkbasketAccessItem(workbasket.getId(), WORKBASKET_OWNER);
    workbasketAccessItem.setAccessName(WORKBASKET_OWNER);
    workbasketAccessItem.setPermission(WorkbasketPermission.APPEND, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.TRANSFER, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.READ, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.OPEN, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.DISTRIBUTE, true);
    workbasketService.createWorkbasketAccessItem(workbasketAccessItem);
  }

  private void createClassificationIfMissing(
      KadaiEngine kadaiEngine, String classificationKey, String domain) throws Exception {
    ClassificationService classificationService = kadaiEngine.getClassificationService();

    try {
      classificationService.getClassification(classificationKey, domain);
    } catch (ClassificationNotFoundException e) {
      Classification classification =
          classificationService.newClassification(classificationKey, domain, "TASK");
      classification.setServiceLevel("P1D");
      classificationService.createClassification(classification);
      LOGGER.info(
          "Created classification {} in domain {} for manual local testing.",
          classificationKey,
          domain);
    }
  }

  private void ensureOutboxSchema(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA IF NOT EXISTS " + OUTBOX_SCHEMA);
      statement.execute(
          "CREATE TABLE IF NOT EXISTS "
              + OUTBOX_SCHEMA
              + ".OUTBOX_SCHEMA_VERSION ("
              + "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
              + "VERSION VARCHAR(255) NOT NULL, "
              + "CREATED TIMESTAMP NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS "
              + OUTBOX_SCHEMA
              + ".EVENT_STORE ("
              + "ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
              + "TYPE VARCHAR(32) NOT NULL, "
              + "CREATED TIMESTAMP, "
              + "PAYLOAD TEXT, "
              + "REMAINING_RETRIES INT NOT NULL, "
              + "BLOCKED_UNTIL TIMESTAMP NOT NULL, "
              + "ERROR VARCHAR(1000), "
              + "CAMUNDA_TASK_ID VARCHAR(40), "
              + "SYSTEM_ENGINE_IDENTIFIER VARCHAR(128), "
              + "LOCK_EXPIRE TIMESTAMP NULL)");
    }

    try (PreparedStatement preparedStatement =
        connection.prepareStatement(
            "INSERT INTO "
                + OUTBOX_SCHEMA
                + ".OUTBOX_SCHEMA_VERSION (VERSION, CREATED) "
                + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM "
                + OUTBOX_SCHEMA
                + ".OUTBOX_SCHEMA_VERSION)")) {
      preparedStatement.setString(1, OUTBOX_SCHEMA_VERSION);
      preparedStatement.setTimestamp(2, Timestamp.from(OffsetDateTime.now().toInstant()));
      preparedStatement.executeUpdate();
    }
  }

  private List<Long> insertManualCreateEvents(Connection connection)
      throws SQLException, JsonProcessingException {
    List<Long> insertedEventIds = new ArrayList<>();

    String insertSql =
        "INSERT INTO "
            + OUTBOX_SCHEMA
            + ".EVENT_STORE (TYPE, CREATED, PAYLOAD, REMAINING_RETRIES, BLOCKED_UNTIL, "
            + "CAMUNDA_TASK_ID, SYSTEM_ENGINE_IDENTIFIER) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement preparedStatement =
        connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

      OffsetDateTime baseTimestamp = OffsetDateTime.now();

      for (int i = 1; i <= MANUAL_OUTBOX_EVENT_COUNT; i++) {
        OffsetDateTime created = baseTimestamp.plusSeconds(i);
        String camundaTaskId = buildCamundaTaskId(i);

        preparedStatement.setString(1, CREATE_EVENT_TYPE);
        preparedStatement.setTimestamp(2, Timestamp.from(created.toInstant()));
        preparedStatement.setString(3, createReferencedTaskPayload(i, camundaTaskId, created));
        preparedStatement.setInt(4, INITIAL_REMAINING_RETRIES);
        preparedStatement.setTimestamp(5, Timestamp.from(created.minusSeconds(1).toInstant()));
        preparedStatement.setString(6, camundaTaskId);
        preparedStatement.setString(7, getSystemEngineIdentifierOrNull());
        preparedStatement.executeUpdate();

        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            insertedEventIds.add(generatedKeys.getLong(1));
          }
        }
      }
    }

    return insertedEventIds;
  }

  private String createReferencedTaskPayload(
      int index, String camundaTaskId, OffsetDateTime created) throws JsonProcessingException {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId(camundaTaskId);
    referencedTask.setName("Manual Camunda outbox task " + index);
    referencedTask.setAssignee("demo");
    referencedTask.setCreated(formatDate(created));
    referencedTask.setDescription(
        "Generated by KadaiAdapterApplication for local Camunda outbox ingestion testing.");
    referencedTask.setOwner("demo");
    referencedTask.setPriority("50");
    referencedTask.setManualPriority("0");
    referencedTask.setSuspended("false");
    referencedTask.setSystemUrl(camundaSystemUrl);
    referencedTask.setTaskDefinitionKey(defaultTaskDefinitionKey);
    referencedTask.setBusinessProcessId("manual-process-" + index);
    referencedTask.setVariables("{}");
    referencedTask.setTaskState("CREATED");
    referencedTask.setDomain(defaultDomain);
    referencedTask.setClassificationKey(defaultClassificationKey);
    referencedTask.setWorkbasketKey(defaultWorkbasketKey);

    return objectMapper.writeValueAsString(referencedTask);
  }

  private String getSystemEngineIdentifierOrNull() {
    return systemEngineIdentifier == null || systemEngineIdentifier.isBlank()
        ? null
        : systemEngineIdentifier;
  }

  private String buildCamundaTaskId(int index) {
    return "manual-camunda-task-"
        + index
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);
  }

  private String formatDate(OffsetDateTime timestamp) {
    return OUTBOX_PAYLOAD_DATE_FORMATTER.format(timestamp);
  }
}
