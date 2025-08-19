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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.camunda.outbox.rest.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.CamundaTaskEventListResource;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Unit test class for Camunda System Connector.
 *
 * @author bbr
 */
@ExtendWith(MockitoExtension.class)
class RetrieveCamundaTaskAccTest {

  @Mock RestClient restClient;

  @Mock HttpHeaderProvider httpHeaderProvider;

  @Mock ObjectMapper objectMapper;

  @InjectMocks CamundaTaskRetriever taskRetriever;

  @Test
  void should_GetActiveCamundaTask() throws JsonProcessingException {

    HttpHeaders mockHeaders = new HttpHeaders();
    when(httpHeaderProvider.getHttpHeadersForOutboxRestApi()).thenReturn(mockHeaders);

    String taskId = "801aca2e-1b25-11e9-b283-94819a5b525c";
    String timeStamp = "2019-01-14T15:22:30.811+0000";

    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setId(taskId);
    expectedTask.setCreated(timeStamp);
    expectedTask.setName("modify Request");
    expectedTask.setAssignee("admin");
    expectedTask.setOwner("admin");
    expectedTask.setDescription("blabla");
    expectedTask.setPriority("50");
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
    expectedTask.setOutboxEventId("1");
    expectedTask.setOutboxEventType("create");

    when(objectMapper.readValue(any(String.class), eq(ReferencedTask.class)))
        .thenReturn(expectedTask);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(contains("/events?type=create"))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(CamundaTaskEventListResource.class))
        .thenReturn(ResponseEntity.ok(createMockCamundaTaskEventListResource("create", 1, taskId)));

    List<ReferencedTask> actualResult =
        taskRetriever.retrieveNewStartedCamundaTasks("http://localhost:8080", "default", null);

    assertThat(actualResult).isNotEmpty();
    assertThat(actualResult.get(0)).isEqualTo(expectedTask);
  }

  @Test
  void should_GetFinishedCamundaTask() throws JsonProcessingException {

    HttpHeaders mockHeaders = new HttpHeaders();
    when(httpHeaderProvider.getHttpHeadersForOutboxRestApi()).thenReturn(mockHeaders);

    String taskId = "2275fb87-1065-11ea-a7a0-02004c4f4f50";

    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setId(taskId);
    expectedTask.setOutboxEventId("16");
    expectedTask.setOutboxEventType("complete");

    when(objectMapper.readValue(any(String.class), eq(ReferencedTask.class)))
        .thenReturn(expectedTask);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(contains("/events?type=complete"))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(CamundaTaskEventListResource.class))
        .thenReturn(
            ResponseEntity.ok(createMockCamundaTaskEventListResource("complete", 16, taskId)));

    List<ReferencedTask> actualResult =
        taskRetriever.retrieveFinishedCamundaTasks("http://localhost:8080", "default", null);

    assertThat(actualResult).isNotEmpty();
    assertThat(actualResult.get(0)).isEqualTo(expectedTask);
  }

  private CamundaTaskEventListResource createMockCamundaTaskEventListResource(
      String type, int id, String payloadId) {

    CamundaTaskEvent event = new CamundaTaskEvent();
    event.setId(id);
    event.setType(type);
    event.setSystemEngineIdentifier("default");
    event.setCreated("1970-01-01T10:48:16.436+0100");

    if ("create".equals(type)) {
      event.setPayload(
          "{\"id\":\""
              + payloadId
              + "\","
              + "\"created\":\"2019-01-14T15:22:30.811+0000\","
              + "\"priority\":\"50\","
              + "\"name\":\"modify Request\","
              + "\"assignee\":\"admin\","
              + "\"description\":\"blabla\","
              + "\"owner\":\"admin\","
              + "\"taskDefinitionKey\":\"Task_0yogl0i\","
              + "\"classificationKey\":\"Schaden_1\","
              + "\"domain\":\"DOMAIN_B\","
              + "\"customInt1\":\"1\","
              + "\"customInt2\":\"2\","
              + "\"customInt3\":\"3\","
              + "\"customInt4\":\"4\","
              + "\"customInt5\":\"5\","
              + "\"customInt6\":\"6\","
              + "\"customInt7\":\"7\","
              + "\"customInt8\":\"8\"}");
    } else {
      event.setPayload("{\"id\":\"" + payloadId + "\"}");
    }

    CamundaTaskEventListResource resource = new CamundaTaskEventListResource();
    List<CamundaTaskEvent> events = new ArrayList<>();

    events.add(event);
    resource.setCamundaTaskEvents(events);

    return resource;
  }
}
