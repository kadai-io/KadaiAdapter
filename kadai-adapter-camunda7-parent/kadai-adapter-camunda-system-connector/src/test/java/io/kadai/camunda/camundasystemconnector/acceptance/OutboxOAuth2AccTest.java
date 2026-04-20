package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import java.io.IOException;
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
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
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
      JacksonAutoConfiguration.class,
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
}
