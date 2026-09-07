package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.models.OutboxEventCountRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.io.IOException;
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
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class Camunda7OutboxHealthIndicatorTest {

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
  void should_ReturnUp_When_OutboxReturnsZero() {
    enqueueJson(200, "{\"eventsCount\":0}");

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(((OutboxEventCountRepresentationModel) health.getDetails().get("outboxService"))
            .getEventsCount())
        .isZero();
  }

  @Test
  void should_ReturnUp_When_OutboxReturnsPositiveCount() {
    enqueueJson(200, "{\"eventsCount\":7}");

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(((OutboxEventCountRepresentationModel) health.getDetails().get("outboxService"))
            .getEventsCount())
        .isEqualTo(7);
  }

  @Test
  void should_ReturnDown_When_OutboxResponseOmitsEventsCount() {
    assertDownForSemanticMismatch("{}");
  }

  @Test
  void should_ReturnDown_When_OutboxResponseContainsNullEventsCount() {
    assertDownForSemanticMismatch("{\"eventsCount\":null}");
  }

  @Test
  void should_ReturnDown_When_OutboxResponseContainsNegativeCount() {
    assertDownForSemanticMismatch("{\"eventsCount\":-1}");
  }

  @Test
  void should_ReturnDown_When_OutboxReturns200WithEmptyBody() {
    enqueueJson(200, "");

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "invalid-response")
        .containsEntry("httpStatus", 200);
  }

  @Test
  void should_ReturnDown_When_OutboxReturns200WithMalformedJson() {
    enqueueJson(200, "{not-json");

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "invalid-response")
        .containsEntry("httpStatus", 200);
  }

  @ParameterizedTest
  @MethodSource("non200Statuses")
  void should_ReturnDownAndExposeStatus_When_OutboxReturnsNon200(int status) {
    enqueueJson(status, "ignored");

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "http-status")
        .containsEntry("httpStatus", status);
  }

  @Test
  void should_SendAuthenticationHeaders_When_PingingOutbox() throws InterruptedException {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.setBasicAuth("user", "pass");
    when(httpHeaderProvider.outboxRestApiHeaders()).thenReturn(authHeaders);
    enqueueJson(200, "{\"eventsCount\":0}");

    Health health = indicator(httpHeaderProvider).health();
    RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(request).isNotNull();
    assertThat(request.getHeader("Authorization")).isEqualTo("Basic dXNlcjpwYXNz");
  }

  @Test
  void should_ReturnDown_When_OutboxCannotBeReached() throws IOException {
    Camunda7OutboxHealthIndicator healthIndicator = indicator();
    mockWebServer.shutdown();
    mockWebServer = null;

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "transport-error")
        .doesNotContainKey("httpStatus");
  }

  private void assertDownForSemanticMismatch(String body) {
    enqueueJson(200, body);

    Health health = indicator().health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("failureType", "semantic-mismatch")
        .containsEntry("httpStatus", 200);
  }

  private Camunda7OutboxHealthIndicator indicator() {
    return indicator(mockHttpHeaderProvider());
  }

  private Camunda7OutboxHealthIndicator indicator(HttpHeaderProvider httpHeaderProvider) {
    return new Camunda7OutboxHealthIndicator(
        new ExternalServiceHttpProbe(restClient, new JsonMapper()),
        httpHeaderProvider,
        mockWebServer.url("/outbox-rest").toString());
  }

  private HttpHeaderProvider mockHttpHeaderProvider() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    when(httpHeaderProvider.outboxRestApiHeaders()).thenReturn(new HttpHeaders());
    return httpHeaderProvider;
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
