/*
 * Copyright [2026] [envite consulting GmbH]
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
 */

package io.kadai.adapter.systemconnector.camunda.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

class Camunda7TaskEventCleanerTest {

  private MockWebServer mockWebServer;
  private Camunda7TaskEventCleaner cleaner;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    when(httpHeaderProvider.getHttpHeadersForOutboxRestApi()).thenReturn(new HttpHeaders());
    cleaner = new Camunda7TaskEventCleaner(httpHeaderProvider, RestClient.create());
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void should_SendEventIds_When_CleaningEvents() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    cleaner.cleanEvents(List.of(3, 5, 8), getServerUrl());

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/events/delete-successful-events");
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"taskCreationIds\":[3,5,8]}");
  }

  @Test
  void should_NotSendRequest_When_EventIdsAreEmpty() {
    cleaner.cleanEvents(List.of(), getServerUrl());

    assertThat(mockWebServer.getRequestCount()).isZero();
  }

  private String getServerUrl() {
    String serverUrl = mockWebServer.url("/").toString();
    return serverUrl.substring(0, serverUrl.length() - 1);
  }
}
