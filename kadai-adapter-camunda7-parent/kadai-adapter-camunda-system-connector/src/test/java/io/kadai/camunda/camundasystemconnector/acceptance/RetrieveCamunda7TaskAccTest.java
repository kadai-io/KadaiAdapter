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

package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.InboundReferencedTask;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7InboundReferencedTask;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.mapper.Camunda7ReferencedTaskMapper;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import io.kadai.task.api.TaskState;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

/**
 * Unit test class for Camunda System Connector.
 *
 * @author bbr
 */
@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest(
    classes = {
      JacksonAutoConfiguration.class,
      Camunda7TaskRetriever.class,
      Camunda7ReferencedTaskMapper.class,
      HttpHeaderProvider.class,
      Camunda7SystemConnectorConfiguration.class,
      AdapterSpringContextProvider.class
    })
class RetrieveCamunda7TaskAccTest {

  @Autowired Camunda7TaskRetriever camunda7TaskRetriever;

  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() {
    mockWebServer = new MockWebServer();
    try {
      mockWebServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void tearDown() {
    try {
      mockWebServer.shutdown();
    } catch (Exception e) {
      // ignore
    }
  }

  @Test
  void should_GetActiveCamundaTask() {
    String timeStamp = "2019-01-14T15:22:30.811+0000";
    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setId("801aca2e-1b25-11e9-b283-94819a5b525c");
    expectedTask.setName("modify Request");
    expectedTask.setAssignee("admin");
    expectedTask.setDescription("blabla");
    expectedTask.setCreated(timeStamp);
    expectedTask.setPlanned("2019-01-15T15:22:30.811+0000");
    expectedTask.setTaskDefinitionKey("Task_0yogl0i");
    expectedTask.setClassificationKey("Schaden_1");
    expectedTask.setDomain("DOMAIN_B");
    expectedTask.setCustomInt1("1");
    expectedTask.setCustomInt2("2");
    expectedTask.setCustomInt3("3");
    expectedTask.setCustomInt4("4");
    expectedTask.setCustomInt5("5");
    expectedTask.setCustomInt6("6");
    expectedTask.setCustomInt7("7");
    expectedTask.setCustomInt8("8");
    String expectedReplyBody =
        "{"
            + "\n"
            + "\"camunda7TaskEvents\":"
            + "[\n "
            + " {\n"
            + "   \"id\": 1,\n"
            + "   \"type\": \"create\",\n"
            + "   \"systemEngineIdentifier\": \"default\",\n"
            + "   \"created\": \"1970-01-01T10:48:16.436+0100\",\n"
            + "   \"payload\": "
            + " \"{\\\"id\\\":\\\"801aca2e-1b25-11e9-b283-94819a5b525c\\\","
            + "            \\\"created\\\":\\\"2019-01-14T15:22:30.811+0000\\\","
            + "            \\\"planned\\\":\\\"2019-01-15T15:22:30.811+0000\\\","
            + "            \\\"priority\\\":\\\"50\\\","
            + "            \\\"name\\\":\\\"modify Request\\\","
            + "            \\\"assignee\\\":\\\"admin\\\","
            + "            \\\"description\\\":\\\"blabla\\\","
            + "            \\\"owner\\\":\\\"admin\\\","
            + "            \\\"taskDefinitionKey\\\":\\\"Task_0yogl0i\\\","
            + "            \\\"classificationKey\\\":\\\"Schaden_1\\\","
            + "            \\\"domain\\\":\\\"DOMAIN_B\\\","
            + "            \\\"customInt1\\\":\\\"1\\\","
            + "            \\\"customInt2\\\":\\\"2\\\","
            + "            \\\"customInt3\\\":\\\"3\\\","
            + "            \\\"customInt4\\\":\\\"4\\\","
            + "            \\\"customInt5\\\":\\\"5\\\","
            + "            \\\"customInt6\\\":\\\"6\\\","
            + "            \\\"customInt7\\\":\\\"7\\\","
            + "            \\\"customInt8\\\":\\\"8\\\""
            + "           }\"\n"
            + " }\n"
            + "]"
            + "}";
    String camundaSystemUrl = mockWebServer.url("/").toString();
    String systemEngineIdentifier = "default";
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(expectedReplyBody));
    List<InboundReferencedTask> actualResult = null;
    try {
      actualResult =
          camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(
              camundaSystemUrl, systemEngineIdentifier, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertThat(actualResult).isNotEmpty();
    Camunda7InboundReferencedTask inboundTask = (Camunda7InboundReferencedTask) actualResult.get(0);
    ReferencedTask actualTask = inboundTask.referencedTask();
    assertThat(actualTask).isEqualTo(expectedTask);
    assertThat(inboundTask.taskEventId()).isEqualTo(1);
    assertThat(actualTask.getPlanned()).isEqualTo("2019-01-15T15:22:30.811+0000");
    assertThat(actualTask.getCustomInt8()).isEqualTo("8");
  }

  @Test
  void should_GetFinishedCamundaTask() {
    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setId("2275fb87-1065-11ea-a7a0-02004c4f4f50");
    expectedTask.setTaskState(TaskState.COMPLETED);
    String expectedReplyBody =
        "{"
            + "\n"
            + "\"camunda7TaskEvents\":"
            + "[\n"
            + "    {\n"
            + "        \"id\": 16,\n"
            + "        \"type\": \"complete\",\n"
            + "        \"systemEngineIdentifier\": \"default\",\n"
            + "        \"created\": \"2019-11-26T16:55:52.460+0100\",\n"
            + "        \"payload\": \"{\\\"id\\\":\\\"2275fb87-1065-11ea-a7a0-02004c4f4f50\\\","
            + "\\\"taskState\\\":\\\"COMPLETED\\\"}\"\n"
            + "    }\n"
            + "]"
            + "}";
    String camundaSystemUrl = mockWebServer.url("").toString();
    String camundaSystemEngineIdentifier = "default";
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(expectedReplyBody));
    List<InboundReferencedTask> actualResult =
        camunda7TaskRetriever.retrieveFinishedCamunda7Tasks(
            camundaSystemUrl, camundaSystemEngineIdentifier, null);
    assertThat(actualResult).isNotEmpty();
    Camunda7InboundReferencedTask inboundTask = (Camunda7InboundReferencedTask) actualResult.get(0);
    ReferencedTask actualTask = inboundTask.referencedTask();
    assertThat(actualTask).isEqualTo(expectedTask);
    assertThat(actualTask.getTaskState()).isEqualTo(TaskState.COMPLETED);
    assertThat(inboundTask.taskEventId()).isEqualTo(16);
  }

  @Test
  void should_IgnoreLegacyPayloadMetadataAndUseEnvelopeMetadata() {
    String expectedReplyBody =
        "{\"camunda7TaskEvents\":[{\"id\":12,\"type\":\"create\","
            + "\"systemEngineIdentifier\":\"default\",\"payload\":"
            + "\"{\\\"id\\\":\\\"task-1\\\",\\\"name\\\":\\\"test\\\","
            + "\\\"outboxEventId\\\":\\\"legacy-id\\\",\\\"outboxEventType\\\":\\\"legacy-type\\\","
            + "\\\"systemUrl\\\":\\\"https://legacy.example\\\"}\"}]}";
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(expectedReplyBody));

    List<InboundReferencedTask> actualResult =
        camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(
            mockWebServer.url("/").toString(), "default", null);

    Camunda7InboundReferencedTask inboundTask = (Camunda7InboundReferencedTask) actualResult.get(0);
    ReferencedTask actualTask = inboundTask.referencedTask();
    assertThat(actualTask.getId()).isEqualTo("task-1");
    assertThat(inboundTask.taskEventId()).isEqualTo(12);
    assertThat(actualTask.getSystemUrl()).isNull();
  }

  @Test
  void should_IgnoreInvalidTaskStateAndContinue_When_RetrievingFinishedCamundaTasks() {
    String expectedReplyBody =
        "{\"camunda7TaskEvents\":["
            + "{\"id\":16,\"type\":\"complete\",\"systemEngineIdentifier\":\"default\","
            + "\"payload\":\"{\\\"id\\\":\\\"task-invalid\\\","
            + "\\\"taskState\\\":\\\"CANCELED\\\"}\"},"
            + "{\"id\":17,\"type\":\"complete\",\"systemEngineIdentifier\":\"default\","
            + "\"payload\":\"{\\\"id\\\":\\\"task-valid\\\","
            + "\\\"taskState\\\":\\\"COMPLETED\\\"}\"}]}";
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(expectedReplyBody));

    List<InboundReferencedTask> actualResult =
        camunda7TaskRetriever.retrieveFinishedCamunda7Tasks(
            mockWebServer.url("").toString(), "default", null);

    assertThat(actualResult).hasSize(1);
    Camunda7InboundReferencedTask inboundTask = (Camunda7InboundReferencedTask) actualResult.get(0);
    assertThat(inboundTask.taskEventId()).isEqualTo(17);
    assertThat(inboundTask.referencedTask().getId()).isEqualTo("task-valid");
    assertThat(inboundTask.referencedTask().getTaskState()).isEqualTo(TaskState.COMPLETED);
  }

  @Test
  void should_EncodeEngineFilterForCreateAndFinishedRequests() throws Exception {
    String engine = "A&B +/%ä?#=engine ";
    String url = mockWebServer.url("").toString();
    for (boolean create : List.of(true, false)) {
      mockWebServer.enqueue(
          new MockResponse()
              .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .setBody("{\"camunda7TaskEvents\":[]}"));

      if (create) {
        assertThat(
                camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(
                    url, engine, Duration.ofSeconds(30)))
            .isEmpty();
      } else {
        assertThat(
                camunda7TaskRetriever.retrieveFinishedCamunda7Tasks(
                    url, engine, Duration.ofSeconds(30)))
            .isEmpty();
      }

      var request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
      assertThat(request).isNotNull();
      assertThat(request.getRequestUrl().queryParameter("engine")).isEqualTo(engine);
      assertThat(request.getRequestUrl().queryParameter("lock-for")).isEqualTo("30");
      assertThat(request.getRequestUrl().queryParameterValues("type"))
          .containsExactlyElementsOf(create ? List.of("create") : List.of("complete", "delete"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void should_OmitEngineParameterWhenNoEngineIsConfigured(boolean create) throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(
                "{\"camunda7TaskEvents\":["
                    + "{\"id\":1,\"systemEngineIdentifier\":null,"
                    + "\"payload\":\"{\\\"id\\\":\\\"null-engine\\\"}\"},"
                    + "{\"id\":2,\"systemEngineIdentifier\":\"A\","
                    + "\"payload\":\"{\\\"id\\\":\\\"engine-A\\\"}\"}]}"));

    String url = mockWebServer.url("").toString();
    List<InboundReferencedTask> tasks =
        create
            ? camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(url, null, null)
            : camunda7TaskRetriever.retrieveFinishedCamunda7Tasks(url, null, null);

    assertThat(tasks)
        .extracting(task -> task.referencedTask().getId())
        .containsExactly("null-engine", "engine-A");
    var request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getRequestUrl().queryParameter("engine")).isNull();
  }

  @Test
  void should_ReportServerResponseContainingAnotherEngine() {
    mockWebServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(
                "{\"camunda7TaskEvents\":[{\"id\":1,\"type\":\"create\","
                    + "\"systemEngineIdentifier\":\"B\",\"payload\":\"{}\"}]}"));

    assertThatThrownBy(
            () ->
                camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(
                    mockWebServer.url("").toString(), "A", null))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "A\nB", "A\tB", "A\u007fB"})
  void should_RejectInvalidEngineBeforeSendingRequest(String engine) {
    String url = mockWebServer.url("").toString();
    assertThatThrownBy(
            () -> camunda7TaskRetriever.retrieveNewStartedCamunda7Tasks(url, engine, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> camunda7TaskRetriever.retrieveFinishedCamunda7Tasks(url, engine, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(mockWebServer.getRequestCount()).isZero();
  }
}
