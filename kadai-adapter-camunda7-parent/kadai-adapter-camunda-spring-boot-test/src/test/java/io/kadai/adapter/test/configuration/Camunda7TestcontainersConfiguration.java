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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

/**
 * Boots a singleton PostgreSQL and Camunda BPM Run container pair used by every integration test.
 *
 * <p>The {@link #initialize()} method is invoked from a static initializer of {@code
 * AbsIntegrationTest} so it runs once per Surefire JVM, before Spring loads.
 *
 * <p>Topology:
 *
 * <pre>
 *   ┌──────────── Docker Network ────────────┐
 *   │                                        │
 *   │  ┌────────────────┐  ┌──────────────┐  │
 *   │  │  PostgreSQL    │  │  Camunda     │  │
 *   │  │  alias=db      │◄─┤  BPM Run     │  │
 *   │  │  port 5432     │  │  port 8080   │  │
 *   │  └─────▲──────────┘  └──────────────┘  │
 *   │        │                               │
 *   └────────┼───────────────────────────────┘
 *            │ mapped on host
 *   ┌────────┴────────────────────────────────┐
 *   │   Test JVM                              │
 *   │   - kadai-adapter  (calls engine-rest)  │
 *   │   - outbox REST (Jersey on port 10020)  │
 *   └─────────────────────────────────────────┘
 * </pre>
 *
 * <p>The Camunda BPM Run container executes BPM processes and, via the {@link
 * io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin} dropped into its
 * userlib, writes outbox events to the shared PostgreSQL database. The test JVM acts as the REST
 * consumer: it reads those events directly through a Jersey JAX-RS server at {@code
 * http://localhost:10020/outbox-rest} (served by OutboxRestJerseyConfig).
 */
public final class Camunda7TestcontainersConfiguration {

  public static final String POSTGRES_IMAGE = "postgres:16-alpine";
  public static final String CAMUNDA_IMAGE = "camunda/camunda-bpm-platform:run-7.23.0";
  public static final String DB_ALIAS = "db";
  public static final String DB_NAME = "camunda";
  public static final String DB_USER = "camunda";
  public static final String DB_PASS = "camunda";
  public static final String OUTBOX_SCHEMA = "kadai_tables";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(Camunda7TestcontainersConfiguration.class);

  private static final Path PROCESSES_DIR =
      Paths.get("src", "main", "resources", "processes").toAbsolutePath();
  private static final Path USERLIB_DIR = Paths.get("target", "camunda-userlib").toAbsolutePath();

  private static volatile boolean initialised = false;

  private static Network network;
  private static PostgreSQLContainer<?> postgres;
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
    LOGGER.info("Starting Camunda BPM Run + PostgreSQL test containers");

    network = Network.newNetwork();

    postgres =
        new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(DB_ALIAS)
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASS);
    postgres.start();

    // Properties file used by the listener inside the Camunda container. The listener
    // looks at -Dkadai.outbox.properties first, falling back to the bundled classpath
    // resource. We give it the in-network Postgres URL.
    String outboxPropsForContainer =
        toPropertiesString("jdbc:postgresql://" + DB_ALIAS + ":5432/" + DB_NAME, DB_USER, DB_PASS);

    camunda =
        new GenericContainer<>(CAMUNDA_IMAGE)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + DB_ALIAS + ":5432/" + DB_NAME)
            .withEnv("SPRING_DATASOURCE_USERNAME", DB_USER)
            .withEnv("SPRING_DATASOURCE_PASSWORD", DB_PASS)
            .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver")
            .withEnv("CAMUNDA_BPM_DATABASE_TYPE", "postgres")
            // Disable the built-in invoice example: its @PostDeploy hook starts invoice process
            // instances whose user tasks have no Kadai extension properties, which causes
            // KadaiTaskListener to throw and crash the container on startup.
            .withEnv("CAMUNDA_BPM_RUN_EXAMPLE_ENABLED", "false")
            // Tell the listener where to find its outbox properties.
            .withEnv(
                "JAVA_OPTS",
                "-Dkadai.outbox.properties=/camunda/configuration/kadai-outbox.properties")
            .withCopyToContainer(
                Transferable.of(outboxPropsForContainer.getBytes()),
                "/camunda/configuration/kadai-outbox.properties")
            .waitingFor(
                Wait.forHttp("/engine-rest/engine")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    mountUserlib(camunda);
    mountProcesses(camunda);

    // Forward Camunda BPM Run container output to SLF4J so ENGINE errors appear in test logs.
    camunda.withLogConsumer(
        new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("CAMUNDA-CONTAINER"))
            .withSeparateOutputStreams());

    camunda.dependsOn(postgres);
    camunda.start();

    String camundaHost = camunda.getHost();
    Integer camundaPort = camunda.getMappedPort(8080);
    String camundaBaseUrl = "http://" + camundaHost + ":" + camundaPort;
    String camundaRestUrl = camundaBaseUrl + "/engine-rest";
    // The outbox REST is served by THIS test JVM's Jersey server (OutboxRestJerseyConfig),
    // NOT by the Camunda container. The adapter calls http://localhost:10020/outbox-rest.
    String outboxUrl = "http://localhost:10020/outbox-rest";

    String hostJdbcUrl =
        "jdbc:postgresql://"
            + postgres.getHost()
            + ":"
            + postgres.getMappedPort(5432)
            + "/"
            + DB_NAME;

    // Make a host-side outbox properties file for the test JVM (used by OutboxRestConfiguration
    // and by Camunda7ListenerConfiguration if either is touched in this JVM).
    Path hostOutboxProps = writeHostOutboxProperties(hostJdbcUrl);
    System.setProperty("kadai.outbox.properties", hostOutboxProps.toString());

    // Spring datasource for the camundaBpmDataSource bean (used by DbCleaner).
    System.setProperty("camunda.datasource.jdbcUrl", hostJdbcUrl);
    System.setProperty("camunda.datasource.url", hostJdbcUrl);
    System.setProperty("camunda.datasource.username", DB_USER);
    System.setProperty("camunda.datasource.password", DB_PASS);
    System.setProperty("camunda.datasource.driverClassName", "org.postgresql.Driver");

    // Spring datasource for the kadai-adapter outbox properties (test JVM perspective).
    System.setProperty("kadai.adapter.outbox.datasource.driver", "org.postgresql.Driver");
    System.setProperty("kadai.adapter.outbox.datasource.url", hostJdbcUrl);
    System.setProperty("kadai.adapter.outbox.datasource.username", DB_USER);
    System.setProperty("kadai.adapter.outbox.datasource.password", DB_PASS);

    // Camunda REST URL consumed by the kadai-adapter Camunda 7 plugin.
    System.setProperty("kadai-adapter.plugin.camunda7.systems[0].system-rest-url", camundaRestUrl);
    // Outbox REST is served by the test JVM's Jersey server at localhost:10020.
    System.setProperty("kadai-adapter.plugin.camunda7.systems[0].system-task-event-url", outboxUrl);

    LOGGER.info("Camunda REST URL: {}", camundaRestUrl);
    LOGGER.info("Outbox REST URL (test JVM): {}", outboxUrl);
    LOGGER.info("Postgres JDBC URL (host): {}", hostJdbcUrl);

    initialised = true;
  }

  private static void mountUserlib(GenericContainer<?> container) {
    if (!Files.isDirectory(USERLIB_DIR)) {
      throw new IllegalStateException(
          "Userlib staging dir not found: "
              + USERLIB_DIR
              + ". Did the maven-dependency-plugin 'stage-camunda-userlib' execution run?");
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

  private static Path writeHostOutboxProperties(String jdbcUrl) {
    Path file = Paths.get("target", "kadai-outbox-host.properties").toAbsolutePath();
    String contents = toPropertiesString(jdbcUrl, DB_USER, DB_PASS);
    try {
      Files.createDirectories(file.getParent());
      try (OutputStream out = Files.newOutputStream(file)) {
        out.write(contents.getBytes());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write " + file, e);
    }
    return file;
  }

  private static String toPropertiesString(String jdbcUrl, String user, String pass) {
    Properties p = new Properties();
    p.setProperty("kadai.adapter.outbox.schema", OUTBOX_SCHEMA);
    p.setProperty("kadai.adapter.outbox.max.number.of.events", "57");
    p.setProperty("kadai.adapter.create_outbox_schema", "true");
    p.setProperty("kadai.adapter.outbox.initial.number.of.task.creation.retries", "3");
    p.setProperty("kadai.adapter.outbox.duration.between.task.creation.retries", "PT1S");
    p.setProperty("kadai.adapter.outbox.datasource.driver", "org.postgresql.Driver");
    p.setProperty("kadai.adapter.outbox.datasource.url", jdbcUrl);
    p.setProperty("kadai.adapter.outbox.datasource.username", user);
    p.setProperty("kadai.adapter.outbox.datasource.password", pass);
    StringBuilder sb = new StringBuilder();
    p.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
    return sb.toString();
  }
}
