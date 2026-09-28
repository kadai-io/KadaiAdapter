package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.models.Camunda7EngineInfoRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class Camunda7HealthIndicatorTest {

  private MockWebServer mockWebServer;
  private RestClient restClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    restClient =
        RestClient.builder()
            .requestFactory(
                new HttpComponentsClientHttpRequestFactory(
                    HttpClients.custom()
                        .disableAutomaticRetries()
                        .disableRedirectHandling()
                        .build()))
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  @Test
  void should_ReturnUp_When_CamundaReturns200AndEngineListIsValid() {
    final String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, "[{\"name\":\"default\"}]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("baseUrl", URI.create(systemRestUrl + "/engine"))
        .containsKey("camundaEngines");
  }

  @Test
  void should_ReturnUp_When_CamundaReturns200ForEngineScopedUrl() {
    String systemRestUrl = mockWebServer.url("/rest/engine/default").toString();
    enqueueJson(200, "[{\"name\":\"default\"}]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("baseUrl", URI.create(mockWebServer.url("/rest/engine").toString()))
        .containsKey("camundaEngine")
        .doesNotContainKey("camundaEngines");
  }

  @Test
  void should_ReturnDown_When_CamundaReturns200AndNoEngines() {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, "[]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "semantic-mismatch")
        .containsEntry("httpStatus", 200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"[null]", "[{}]", "[{\"name\":null}]", "[{\"name\":\"   \"}]"})
  void should_ReturnDown_When_CamundaReturnsOnlyInvalidEngines(String body) {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, body);

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "semantic-mismatch")
        .containsEntry("httpStatus", 200);
  }

  @Test
  void should_ReturnUp_When_CamundaReturnsInvalidAndValidEngines() {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, "[null,{}, {\"name\":\"x\"}]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    Camunda7EngineInfoRepresentationModel[] engines =
        (Camunda7EngineInfoRepresentationModel[]) health.getDetails().get("camundaEngines");
    assertThat(engines).hasSize(1);
    assertThat(engines[0].getName()).isEqualTo("x");
  }

  @Test
  void should_ReturnDown_When_ExpectedEngineIsNotListed() {
    String systemRestUrl = mockWebServer.url("/engine-rest/engine/default").toString();
    enqueueJson(200, "[{\"name\":\"other\"}]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("camundaEngineError", "Expected engine 'default' not found")
        .containsEntry("failureType", "semantic-mismatch")
        .containsEntry("httpStatus", 200);
    Camunda7EngineInfoRepresentationModel[] engines =
        (Camunda7EngineInfoRepresentationModel[]) health.getDetails().get("camundaEngines");
    assertThat(engines)
        .extracting(Camunda7EngineInfoRepresentationModel::getName)
        .containsExactly("other");
  }

  @Test
  void should_ReturnDown_When_CamundaReturns200WithEmptyBody() {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, "");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "invalid-response")
        .containsEntry("httpStatus", 200);
  }

  @Test
  void should_ReturnDown_When_CamundaReturns200WithMalformedJson() {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(200, "{not-json");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "invalid-response")
        .containsEntry("httpStatus", 200);
  }

  @ParameterizedTest
  @MethodSource("non200Statuses")
  void should_ReturnDownAndExposeStatus_When_CamundaReturnsNon200(int status) {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(status, "ignored");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "http-status")
        .containsEntry("httpStatus", status);
  }

  @ParameterizedTest
  @ValueSource(ints = {301, 503})
  void should_MakeOnlyOneRequest_When_HealthProbeReceivesRedirectOrRetryStatus(int status)
      throws InterruptedException {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    enqueueJson(status, "ignored");
    enqueueJson(200, "[{\"name\":\"default\"}]");

    Health health = indicator(camunda7System(systemRestUrl)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "http-status")
        .containsEntry("httpStatus", status);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  void should_SendAuthenticationHeaders_When_PingingCamunda() throws InterruptedException {
    final String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.setBasicAuth("user", "pass");
    when(httpHeaderProvider.camunda7RestApiHeaders()).thenReturn(authHeaders);
    enqueueJson(200, "[{\"name\":\"default\"}]");

    Health health = indicator(camunda7System(systemRestUrl), httpHeaderProvider).health();
    RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(request).isNotNull();
    assertThat(request.getHeader("Authorization")).isEqualTo("Basic dXNlcjpwYXNz");
  }

  @Test
  void should_ReturnDown_When_CamundaCannotBeReached() throws IOException {
    String systemRestUrl = mockWebServer.url("/engine-rest").toString();
    Camunda7HealthIndicator healthIndicator = indicator(camunda7System(systemRestUrl));
    mockWebServer.shutdown();
    mockWebServer = null;

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "transport-error")
        .doesNotContainKey("httpStatus");
  }

  private Camunda7HealthIndicator indicator(Camunda7System camunda7System) {
    return indicator(camunda7System, mockHttpHeaderProvider());
  }

  private Camunda7HealthIndicator indicator(
      Camunda7System camunda7System, HttpHeaderProvider httpHeaderProvider) {
    return new Camunda7HealthIndicator(restClient, httpHeaderProvider, camunda7System);
  }

  private HttpHeaderProvider mockHttpHeaderProvider() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    when(httpHeaderProvider.camunda7RestApiHeaders()).thenReturn(new HttpHeaders());
    return httpHeaderProvider;
  }

  private Camunda7System camunda7System(String systemRestUrl) {
    Camunda7System camunda7System = new Camunda7System();
    camunda7System.setSystemRestUrl(systemRestUrl);
    return camunda7System;
  }

  private void enqueueJson(int status, String body) {
    MockResponse response =
        new MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(body);
    if (status == 301) {
      response.setHeader("Location", "/redirected");
    }
    mockWebServer.enqueue(response);
  }

  private static Stream<Integer> non200Statuses() {
    return Stream.of(204, 301, 400, 401, 403, 404, 429, 500, 503);
  }
}
