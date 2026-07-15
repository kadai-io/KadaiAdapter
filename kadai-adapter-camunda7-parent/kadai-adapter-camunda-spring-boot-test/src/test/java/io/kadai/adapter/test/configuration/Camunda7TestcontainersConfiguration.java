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
 */

package io.kadai.adapter.test.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Boots singleton database and Camunda BPM Run containers used by every integration test.
 *
 * <p>The database is selected via the {@code DB} environment variable. Supported values are {@code
 * H2}, {@code POSTGRES}, and {@code ORACLE}. Missing or unsupported values fall back to {@code H2}.
 *
 * <p>The Camunda BPM Run container executes BPM processes and, via the {@link
 * io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin} dropped into its
 * userlib, writes outbox events to the selected database. The test JVM acts as the REST consumer:
 * it reads those events directly through a Jersey JAX-RS server at {@code
 * http://localhost:10020/outbox-rest} (served by OutboxRestJerseyConfig).
 */
public final class Camunda7TestcontainersConfiguration {

  public static final String POSTGRES_IMAGE = "postgres:16-alpine";
  public static final String ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim-faststart";
  public static final String CAMUNDA_IMAGE = "camunda/camunda-bpm-platform:run-7.24.0";
  public static final String DB_ALIAS = "db";
  public static final String DB_NAME = "camunda";
  public static final String POSTGRES_USER = "camunda";
  public static final String POSTGRES_PASS = "camunda";
  public static final String ORACLE_USER = "camunda";
  public static final String ORACLE_PASS = "camundaPwd1";
  public static final String DEFAULT_OUTBOX_SCHEMA = "kadai_tables";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(Camunda7TestcontainersConfiguration.class);

  private static final Path PROCESSES_DIR =
      Paths.get("src", "main", "resources", "processes").toAbsolutePath();
  private static final Path USERLIB_DIR = Paths.get("target", "camunda-userlib").toAbsolutePath();

  private static volatile boolean initialised = false;

  private static Network network;
  private static Server h2Server;
  private static JdbcDatabaseContainer<?> databaseContainer;
  private static GenericContainer<?> camunda;

  private Camunda7TestcontainersConfiguration() {
    // utility holder
  }

  /**
   * Idempotently starts the containers and exports their connection coordinates as system
   * properties so that Spring (started later by {@code @SpringBootTest}) picks them up.
   */
  public static synchronized void initialize() {
    if (initialised) {
      return;
    }

    TestDatabase database = TestDatabase.fromEnvironment();
    LOGGER.info("Starting Camunda BPM Run test infrastructure with {} database", database);

    network = Network.newNetwork();

    JdbcCoordinates jdbc = startDatabase(database);

    // Properties file used by the listener inside the Camunda container. The listener
    // looks at -Dkadai.outbox.properties first, falling back to the bundled classpath
    // resource. We give it the container/network-side JDBC URL.
    String outboxPropsForContainer =
        toPropertiesString(
            jdbc.containerJdbcUrl(),
            jdbc.driverClassName(),
            jdbc.username(),
            jdbc.password(),
            jdbc.outboxSchema());

    camunda =
        new GenericContainer<>(CAMUNDA_IMAGE)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("SPRING_DATASOURCE_URL", jdbc.containerJdbcUrl())
            .withEnv("SPRING_DATASOURCE_USERNAME", jdbc.username())
            .withEnv("SPRING_DATASOURCE_PASSWORD", jdbc.password())
            .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", jdbc.driverClassName())
            .withEnv("CAMUNDA_BPM_DATABASE_TYPE", database.camundaDatabaseType)
            // Disable the built-in invoice example: its @PostDeploy hook starts invoice process
            // instances whose user tasks have no Kadai extension properties, which causes
            // KadaiTaskListener to throw and crash the container on startup.
            .withEnv("CAMUNDA_BPM_RUN_EXAMPLE_ENABLED", "false")
            // Tell the listener where to find its outbox properties.
            .withEnv(
                "JAVA_OPTS",
                "-Dkadai.outbox.properties=/camunda/configuration/kadai-outbox.properties")
            .withCopyToContainer(
                Transferable.of(outboxPropsForContainer.getBytes(StandardCharsets.UTF_8)),
                "/camunda/configuration/kadai-outbox.properties")
            .waitingFor(
                Wait.forHttp("/engine-rest/engine")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    mountUserlib(camunda);
    mountProcesses(camunda);

    // Forward Camunda BPM Run container output to SLF4J so ENGINE errors appear in test logs.
    camunda.withLogConsumer(
        new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("CAMUNDA-CONTAINER"))
            .withSeparateOutputStreams());

    if (databaseContainer != null) {
      camunda.dependsOn(databaseContainer);
    }
    camunda.start();

    String camundaHost = camunda.getHost();
    Integer camundaPort = camunda.getMappedPort(8080);
    String camundaBaseUrl = "http://" + camundaHost + ":" + camundaPort;
    String camundaRestUrl = camundaBaseUrl + "/engine-rest";
    // The outbox REST is served by THIS test JVM's Jersey server (OutboxRestJerseyConfig),
    // NOT by the Camunda container. The adapter calls http://localhost:10020/outbox-rest.
    String outboxUrl = "http://localhost:10020/outbox-rest";

    // Make a host-side outbox properties file for the test JVM (used by OutboxRestConfiguration
    // and by Camunda7ListenerConfiguration if either is touched in this JVM).
    Path hostOutboxProps =
        writeHostOutboxProperties(
            jdbc.hostJdbcUrl(),
            jdbc.driverClassName(),
            jdbc.username(),
            jdbc.password(),
            jdbc.outboxSchema());
    System.setProperty("kadai.outbox.properties", hostOutboxProps.toString());

    // Spring datasource for the camundaBpmDataSource bean (used by DbCleaner).
    System.setProperty("camunda.datasource.jdbcUrl", jdbc.hostJdbcUrl());
    System.setProperty("camunda.datasource.url", jdbc.hostJdbcUrl());
    System.setProperty("camunda.datasource.username", jdbc.username());
    System.setProperty("camunda.datasource.password", jdbc.password());
    System.setProperty("camunda.datasource.driverClassName", jdbc.driverClassName());

    // Spring datasource for the kadai-adapter outbox properties (test JVM perspective).
    System.setProperty("kadai.adapter.outbox.schema", jdbc.outboxSchema());
    System.setProperty("kadai.adapter.outbox.datasource.driver", jdbc.driverClassName());
    System.setProperty("kadai.adapter.outbox.datasource.url", jdbc.hostJdbcUrl());
    System.setProperty("kadai.adapter.outbox.datasource.username", jdbc.username());
    System.setProperty("kadai.adapter.outbox.datasource.password", jdbc.password());

    // Camunda REST URL consumed by the kadai-adapter Camunda 7 plugin.
    System.setProperty("kadai-adapter.plugin.camunda7.systems[0].system-rest-url", camundaRestUrl);
    // Outbox REST is served by the test JVM's Jersey server at localhost:10020.
    System.setProperty("kadai-adapter.plugin.camunda7.systems[0].system-task-event-url", outboxUrl);

    LOGGER.info("Camunda REST URL: {}", camundaRestUrl);
    LOGGER.info("Outbox REST URL (test JVM): {}", outboxUrl);
    LOGGER.info("{} JDBC URL (host): {}", database, jdbc.hostJdbcUrl());

    initialised = true;
  }

  public static synchronized void shutdown() {
    if (camunda != null) {
      camunda.stop();
      camunda = null;
    }
    if (databaseContainer != null) {
      databaseContainer.stop();
      databaseContainer = null;
    }
    if (h2Server != null) {
      h2Server.stop();
      h2Server = null;
    }
    if (network != null) {
      network.close();
      network = null;
    }
    initialised = false;
  }

  private static JdbcCoordinates startDatabase(TestDatabase database) {
    return switch (database) {
      case H2 -> startH2();
      case POSTGRES -> startPostgres();
      case ORACLE -> startOracle();
    };
  }

  private static JdbcCoordinates startH2() {
    try {
      h2Server =
          Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "0", "-ifNotExists")
              .start();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to start H2 TCP server", e);
    }

    int h2Port = h2Server.getPort();
    Testcontainers.exposeHostPorts(h2Port);

    String jdbcOptions = ";NON_KEYWORDS=KEY,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    return new JdbcCoordinates(
        "jdbc:h2:tcp://host.testcontainers.internal:" + h2Port + "/mem:" + DB_NAME + jdbcOptions,
        "jdbc:h2:tcp://localhost:" + h2Port + "/mem:" + DB_NAME + jdbcOptions,
        "org.h2.Driver",
        "sa",
        "sa",
        DEFAULT_OUTBOX_SCHEMA);
  }

  private static JdbcCoordinates startPostgres() {
    PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(DB_ALIAS)
            .withDatabaseName(DB_NAME)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASS);
    postgres.start();
    databaseContainer = postgres;

    return new JdbcCoordinates(
        "jdbc:postgresql://" + DB_ALIAS + ":5432/" + DB_NAME,
        postgres.getJdbcUrl(),
        "org.postgresql.Driver",
        POSTGRES_USER,
        POSTGRES_PASS,
        DEFAULT_OUTBOX_SCHEMA);
  }

  private static JdbcCoordinates startOracle() {
    OracleContainer oracle =
        new OracleContainer(DockerImageName.parse(ORACLE_IMAGE))
            .withNetwork(network)
            .withNetworkAliases(DB_ALIAS)
            .withUsername(ORACLE_USER)
            .withPassword(ORACLE_PASS);
    oracle.start();
    databaseContainer = oracle;

    return new JdbcCoordinates(
        "jdbc:oracle:thin:@" + DB_ALIAS + ":1521/" + oracle.getDatabaseName(),
        oracle.getJdbcUrl(),
        oracle.getDriverClassName(),
        oracle.getUsername(),
        oracle.getPassword(),
        oracle.getUsername().toUpperCase(Locale.ROOT));
  }

  private static void mountUserlib(GenericContainer<?> container) {
    if (!Files.isDirectory(USERLIB_DIR)) {
      throw new IllegalStateException(
          "Userlib staging dir not found: "
              + USERLIB_DIR
              + ". Did the maven-dependency-plugin 'stage-camunda-userlib' execution run? "
              + "You can run it with: "
              + "mvn -pl kadai-adapter-camunda7-parent/kadai-adapter-camunda-spring-boot-test "
              + "-am process-test-resources -DskipTests");
    }
    try (Stream<Path> jars = Files.list(USERLIB_DIR)) {
      jars.filter(p -> p.toString().endsWith(".jar"))
          .forEach(
              jar ->
                  container.withCopyFileToContainer(
                      MountableFile.forHostPath(jar.toAbsolutePath().toString()),
                      "/camunda/configuration/userlib/" + jar.getFileName().toString()));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to enumerate userlib jars in " + USERLIB_DIR, e);
    }
  }

  private static void mountProcesses(GenericContainer<?> container) {
    if (!Files.isDirectory(PROCESSES_DIR)) {
      LOGGER.warn("No BPMN processes directory at {} - nothing to deploy", PROCESSES_DIR);
      return;
    }
    try (Stream<Path> bpmns = Files.list(PROCESSES_DIR)) {
      bpmns
          .filter(p -> p.toString().endsWith(".bpmn"))
          .forEach(
              bpmn ->
                  container.withCopyFileToContainer(
                      MountableFile.forHostPath(bpmn.toAbsolutePath().toString()),
                      "/camunda/configuration/resources/" + bpmn.getFileName().toString()));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to enumerate BPMN files in " + PROCESSES_DIR, e);
    }
  }

  private static Path writeHostOutboxProperties(
      String jdbcUrl, String driverClassName, String user, String pass, String outboxSchema) {
    Path file = Paths.get("target", "kadai-outbox-host.properties").toAbsolutePath();
    String contents = toPropertiesString(jdbcUrl, driverClassName, user, pass, outboxSchema);
    try {
      Files.createDirectories(file.getParent());
      try (OutputStream out = Files.newOutputStream(file)) {
        out.write(contents.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write " + file, e);
    }
    return file;
  }

  private static String toPropertiesString(
      String jdbcUrl, String driverClassName, String user, String pass, String outboxSchema) {
    Properties p = new Properties();
    p.setProperty("kadai.adapter.outbox.schema", outboxSchema);
    p.setProperty("kadai.adapter.outbox.max.number.of.events", "57");
    p.setProperty("kadai.adapter.create_outbox_schema", "true");
    p.setProperty("kadai.adapter.outbox.initial.number.of.task.creation.retries", "3");
    p.setProperty("kadai.adapter.outbox.duration.between.task.creation.retries", "PT1S");
    p.setProperty("kadai.adapter.outbox.datasource.driver", driverClassName);
    p.setProperty("kadai.adapter.outbox.datasource.url", jdbcUrl);
    p.setProperty("kadai.adapter.outbox.datasource.username", user);
    p.setProperty("kadai.adapter.outbox.datasource.password", pass);
    StringBuilder sb = new StringBuilder();
    p.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
    return sb.toString();
  }

  private enum TestDatabase {
    H2("h2"),
    POSTGRES("postgres"),
    ORACLE("oracle");

    private final String camundaDatabaseType;

    TestDatabase(String camundaDatabaseType) {
      this.camundaDatabaseType = camundaDatabaseType;
    }

    private static TestDatabase fromEnvironment() {
      String value = System.getenv("DB");
      if (value == null || value.isBlank()) {
        return H2;
      }
      try {
        return TestDatabase.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER.warn("Unsupported DB value '{}'; falling back to H2", value);
        return H2;
      }
    }
  }

  private record JdbcCoordinates(
      String containerJdbcUrl,
      String hostJdbcUrl,
      String driverClassName,
      String username,
      String password,
      String outboxSchema) {}
}
