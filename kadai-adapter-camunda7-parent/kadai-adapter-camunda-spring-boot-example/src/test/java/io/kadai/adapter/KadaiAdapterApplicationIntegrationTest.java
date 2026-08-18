package io.kadai.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.kadai.adapter.impl.scheduled.KadaiTaskStarterOrchestrator;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = KadaiAdapterApplication.class)
@ExtendWith(JaasExtension.class)
class KadaiAdapterApplicationIntegrationTest {

  private static final AtomicBoolean EVENT_IS_AVAILABLE = new AtomicBoolean(false);
  private static final HttpServer OUTBOX_SERVER = startOutboxServer();

  @Autowired private KadaiEngine kadaiEngine;
  @Autowired private KadaiTaskStarterOrchestrator taskStarter;

  @AfterAll
  static void stopOutboxServer() {
    OUTBOX_SERVER.stop(0);
  }

  @DynamicPropertySource
  static void configureAdapter(DynamicPropertyRegistry registry) {
    String baseUrl = "http://localhost:" + OUTBOX_SERVER.getAddress().getPort();
    registry.add(
        "kadai-adapter.plugin.camunda7.systems[0].system-rest-url",
        () -> baseUrl + "/example-context-root/engine-rest");
    registry.add(
        "kadai-adapter.plugin.camunda7.systems[0].system-task-event-url",
        () -> baseUrl + "/example-context-root/outbox-rest");
  }

  @Test
  @WithAccessId(user = "admin")
  void should_CreateKadaiTask_When_CamundaOutboxReturnsCreateEvent() throws Exception {
    KadaiAdapterTestUtil testUtil = new KadaiAdapterTestUtil(kadaiEngine);
    testUtil.createWorkbasket("someWbkey", "DOMAIN_A");
    testUtil.createClassification("L1050", "DOMAIN_A");
    EVENT_IS_AVAILABLE.set(true);

    taskStarter.retrieveNewReferencedTasksAndCreateCorrespondingKadaiTasks();

    var taskSummaries =
        kadaiEngine
            .getTaskService()
            .createTaskQuery()
            .businessProcessIdIn("camunda-process-1")
            .list();
    assertThat(taskSummaries).singleElement();

    Task task = kadaiEngine.getTaskService().getTask(taskSummaries.getFirst().getId());

    assertThat(task.getExternalId()).isNotBlank();
    assertThat(task.getState()).isEqualTo(TaskState.READY);
    assertThat(task.getWorkbasketKey()).isEqualTo("someWbkey");
    assertThat(task.getClassificationSummary().getKey()).isEqualTo("L1050");
  }

  private static HttpServer startOutboxServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext(
          "/example-context-root/outbox-rest/events",
          KadaiAdapterApplicationIntegrationTest::handleOutbox);
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException("Could not start the Camunda outbox test server", e);
    }
  }

  private static void handleOutbox(HttpExchange exchange) throws IOException {
    if ("GET".equals(exchange.getRequestMethod())) {
      String response =
          EVENT_IS_AVAILABLE.get() && exchange.getRequestURI().getQuery().contains("type=create")
              ? createEventResponse()
              : "{\"camunda7TaskEvents\":[]}";
      sendResponse(exchange, 200, response);
      return;
    }

    EVENT_IS_AVAILABLE.set(false);
    sendResponse(exchange, 204, "");
  }

  private static String createEventResponse() {
    return "{\"camunda7TaskEvents\":[{\"id\":1,\"type\":\"create\","
        + "\"systemEngineIdentifier\":\"default\",\"payload\":\""
        + "{\\\"id\\\":\\\"camunda-task-1\\\",\\\"name\\\":\\\"Review request\\\","
        + "\\\"businessProcessId\\\":\\\"camunda-process-1\\\","
        + "\\\"classificationKey\\\":\\\"L1050\\\",\\\"domain\\\":\\\"DOMAIN_A\\\","
        + "\\\"workbasketKey\\\":\\\"someWbkey\\\",\\\"manualPriority\\\":\\\"-1\\\","
        + "\\\"variables\\\":\\\"{}\\\"}\"}]}";
  }

  private static void sendResponse(HttpExchange exchange, int status, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
