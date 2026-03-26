package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEventListResource;
import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * Full integration test demonstrating end-to-end OAuth2 authentication flow for Outbox REST API
 * requests. Uses MockWebServer to simulate both the OAuth2 authorization server and the Outbox REST
 * API.
 *
 * <p>The mock OAuth2 token server is started before the Spring context is created, so that the
 * dynamic port can be injected into the configuration properties via {@link DynamicPropertySource}.
 */
@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest(
    classes = {
      Camunda7TaskRetriever.class,
      HttpHeaderProvider.class,
      Camunda7SystemConnectorConfiguration.class,
      OutboxOAuth2TokenProvider.class,
      AdapterSpringContextProvider.class
    })
class OutboxOAuth2AccTest {

  private static final String TOKEN_RESPONSE =
      """
      {
        "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test-payload.test-signature",
        "token_type": "Bearer",
        "expires_in": 3600,
        "scope": "outbox:read outbox:write"
      }
      """;

  private static MockWebServer mockTokenServer;

  @Autowired private HttpHeaderProvider httpHeaderProvider;

  @Autowired private Camunda7SystemConnectorConfiguration config;

  @Autowired private RestClient restClient;

  private MockWebServer mockOutboxServer;

  @BeforeAll
  static void startTokenServer() throws IOException {
    mockTokenServer = new MockWebServer();
    // Dispatcher that always returns a valid token response for POST /oauth/token
    mockTokenServer.setDispatcher(
        new Dispatcher() {
          @NotNull
          @Override
          public MockResponse dispatch(@NotNull RecordedRequest request) {
            if ("/oauth/token".equals(request.getPath()) && "POST".equals(request.getMethod())) {
              return new MockResponse()
                  .setBody(TOKEN_RESPONSE)
                  .addHeader("Content-Type", "application/json");
            }
            return new MockResponse().setResponseCode(404);
          }
        });
    mockTokenServer.start();
  }

  @AfterAll
  static void stopTokenServer() throws IOException {
    if (mockTokenServer != null) {
      mockTokenServer.shutdown();
    }
  }

  @DynamicPropertySource
  static void registerOAuth2Properties(DynamicPropertyRegistry registry) {
    registry.add("kadai-adapter.plugin.camunda7.outbox.auth-type", () -> "OAUTH2");
    registry.add(
        "kadai-adapter.plugin.camunda7.outbox.oauth2.token-uri",
        () -> mockTokenServer.url("/oauth/token").toString());
    registry.add("kadai-adapter.plugin.camunda7.outbox.oauth2.client-id", () -> "kadai-adapter");
    registry.add("kadai-adapter.plugin.camunda7.outbox.oauth2.client-secret", () -> "super-secret");
    registry.add(
        "kadai-adapter.plugin.camunda7.outbox.oauth2.scopes", () -> "outbox:read outbox:write");
  }

  @BeforeEach
  void setUp() throws IOException {
    mockOutboxServer = new MockWebServer();
    mockOutboxServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockOutboxServer.shutdown();
  }

  @Test
  void should_ProduceBearerHeader_When_OutboxAuthTypeIsOAuth2() {
    HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();

    assertThat(headers.getFirst("Authorization")).isNotNull();
    assertThat(headers.getFirst("Authorization")).startsWith("Bearer ");
    assertThat(headers.getFirst("Authorization")).contains("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
  }

  @Test
  void should_NotAffectCamunda7RestApiHeaders_When_OutboxUsesOAuth2() {
    // Camunda REST API should use its own auth config (basic)
    HttpHeaders camundaHeaders = httpHeaderProvider.camunda7RestApiHeaders();

    // The camunda7 client config uses the defaults from application.properties (demo/demoPwd)
    assertThat(camundaHeaders.getFirst("Authorization")).startsWith("Basic ");
  }

  @Test
  void should_IncludeContentTypeJson_When_CallingGetHttpHeadersForOutboxRestApi() {
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(headers.getFirst("Authorization")).startsWith("Bearer ");
  }

  @Test
  void should_ConfigureOAuth2PropertiesCorrectly() {
    assertThat(config.getOutbox().getAuthType())
        .isEqualTo(Camunda7SystemConnectorConfiguration.AuthType.OAUTH2);
    assertThat(config.getOutbox().getOauth2()).isNotNull();
    assertThat(config.getOutbox().getOauth2().getClientId()).isEqualTo("kadai-adapter");
    assertThat(config.getOutbox().getOauth2().getClientSecret()).isEqualTo("super-secret");
    assertThat(config.getOutbox().getOauth2().getScopes()).isEqualTo("outbox:read outbox:write");
  }

  @Test
  void should_SendBearerTokenToOutbox_When_MakingGetRequest() throws Exception {
    Camunda7TaskEventListResource listResource = new Camunda7TaskEventListResource();
    listResource.setCamunda7TaskEvents(new ArrayList<>());
    ObjectMapper objectMapper = new ObjectMapper();
    String outboxResponseBody = objectMapper.writeValueAsString(listResource);

    mockOutboxServer.enqueue(
        new MockResponse()
            .setBody(outboxResponseBody)
            .addHeader("Content-Type", "application/json"));

    String outboxUrl = mockOutboxServer.url("/outbox-rest/events?type=create").toString();
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    Camunda7TaskEventListResource response =
        restClient
            .get()
            .uri(outboxUrl)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .body(Camunda7TaskEventListResource.class);

    assertThat(response).isNotNull();
    assertThat(response.getCamunda7TaskEvents()).isEmpty();

    RecordedRequest recordedRequest = mockOutboxServer.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).startsWith("Bearer ");
    assertThat(recordedRequest.getHeader("Authorization"))
        .contains("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
    assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
  }

  @Test
  void should_SendBearerTokenForOutboxDelete_When_MakingPostRequest() throws Exception {
    mockOutboxServer.enqueue(new MockResponse().setResponseCode(204));

    String deleteUrl = mockOutboxServer.url("/outbox-rest/events/delete-events").toString();
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    restClient
        .post()
        .uri(deleteUrl)
        .headers(httpHeaders -> httpHeaders.addAll(headers))
        .body("[1, 2, 3]")
        .retrieve()
        .toEntity(Void.class);

    RecordedRequest recordedRequest = mockOutboxServer.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).startsWith("Bearer ");
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void should_SendBearerTokenForOutboxDecreaseRetries_When_MakingPostRequest() throws Exception {
    mockOutboxServer.enqueue(new MockResponse().setResponseCode(200));

    String retriesUrl =
        mockOutboxServer.url("/outbox-rest/events/decrease-remaining-retries").toString();
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    restClient
        .post()
        .uri(retriesUrl)
        .headers(httpHeaders -> httpHeaders.addAll(headers))
        .body("{\"eventId\":\"42\",\"error\":\"some error\"}")
        .retrieve()
        .toEntity(Void.class);

    RecordedRequest recordedRequest = mockOutboxServer.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).startsWith("Bearer ");
  }

  @Test
  void should_ReturnOutboxEvents_When_UsingOAuth2Token() throws Exception {
    Camunda7TaskEvent event1 = new Camunda7TaskEvent();
    event1.setId(1);
    event1.setType("create");
    event1.setPayload("{\"id\":\"task-001\"}");
    event1.setRemainingRetries(3);

    Camunda7TaskEvent event2 = new Camunda7TaskEvent();
    event2.setId(2);
    event2.setType("create");
    event2.setPayload("{\"id\":\"task-002\"}");
    event2.setRemainingRetries(3);

    Camunda7TaskEventListResource listResource = new Camunda7TaskEventListResource();
    List<Camunda7TaskEvent> events = new ArrayList<>();
    events.add(event1);
    events.add(event2);
    listResource.setCamunda7TaskEvents(events);

    ObjectMapper objectMapper = new ObjectMapper();
    String outboxResponseBody = objectMapper.writeValueAsString(listResource);

    mockOutboxServer.enqueue(
        new MockResponse()
            .setBody(outboxResponseBody)
            .addHeader("Content-Type", "application/json"));

    String eventsUrl = mockOutboxServer.url("/outbox-rest/events?type=create").toString();
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    Camunda7TaskEventListResource response =
        restClient
            .get()
            .uri(eventsUrl)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .body(Camunda7TaskEventListResource.class);

    assertThat(response).isNotNull();
    assertThat(response.getCamunda7TaskEvents()).hasSize(2);
    assertThat(response.getCamunda7TaskEvents().get(0).getType()).isEqualTo("create");
    assertThat(response.getCamunda7TaskEvents().get(1).getId()).isEqualTo(2);

    RecordedRequest recordedRequest = mockOutboxServer.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).startsWith("Bearer ");
  }

  @Test
  void should_ReuseToken_When_MultipleOutboxRequestsMade() throws Exception {
    // Enqueue two outbox responses
    Camunda7TaskEventListResource emptyResource = new Camunda7TaskEventListResource();
    emptyResource.setCamunda7TaskEvents(new ArrayList<>());
    ObjectMapper objectMapper = new ObjectMapper();
    String body = objectMapper.writeValueAsString(emptyResource);

    mockOutboxServer.enqueue(
        new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));
    mockOutboxServer.enqueue(
        new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));

    String url1 = mockOutboxServer.url("/outbox-rest/events?type=create").toString();
    String url2 = mockOutboxServer.url("/outbox-rest/events?type=complete&type=delete").toString();

    HttpHeaders headers1 = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    restClient
        .get()
        .uri(url1)
        .headers(h -> h.addAll(headers1))
        .retrieve()
        .body(Camunda7TaskEventListResource.class);

    HttpHeaders headers2 = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    restClient
        .get()
        .uri(url2)
        .headers(h -> h.addAll(headers2))
        .retrieve()
        .body(Camunda7TaskEventListResource.class);

    RecordedRequest req1 = mockOutboxServer.takeRequest();
    RecordedRequest req2 = mockOutboxServer.takeRequest();

    // Both outbox requests should carry the same Bearer token (cached)
    String auth1 = req1.getHeader("Authorization");
    String auth2 = req2.getHeader("Authorization");
    assertThat(auth1).isEqualTo(auth2);
    assertThat(auth1).startsWith("Bearer ");
  }
}
