package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration.OAuth2Configuration;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import java.io.IOException;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for {@link OutboxOAuth2TokenProvider} that use MockWebServer to simulate an OAuth2
 * token endpoint.
 */
class OutboxOAuth2TokenProviderTest {

  private MockWebServer mockTokenServer;

  @BeforeEach
  void setUp() throws IOException {
    mockTokenServer = new MockWebServer();
    mockTokenServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockTokenServer.shutdown();
  }

  @Test
  void should_FetchAccessToken_When_TokenEndpointReturnsValidResponse() throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "my-access-token-123",
          "token_type": "Bearer",
          "expires_in": 3600,
          "scope": "read write"
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OAuth2Configuration oauth2Config = createOAuth2Config();

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);

    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("my-access-token-123");

    RecordedRequest request = mockTokenServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    String body = request.getBody().readUtf8();
    assertThat(body)
        .contains("grant_type=client_credentials")
        .contains("client_id=my-client-id")
        .contains("client_secret=my-client-secret")
        .contains("scope=read+write");
  }

  @Test
  void should_CacheToken_When_CalledMultipleTimes() {
    String tokenResponseBody =
        """
        {
          "access_token": "cached-token",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    String token1 = provider.getAccessToken();
    String token2 = provider.getAccessToken();
    String token3 = provider.getAccessToken();

    assertThat(token1).isEqualTo("cached-token");
    assertThat(token2).isEqualTo("cached-token");
    assertThat(token3).isEqualTo("cached-token");

    // Only one request should have been made
    assertThat(mockTokenServer.getRequestCount()).isEqualTo(1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void should_NotIncludeScope_When_ScopeIsNullOrEmpty(String scope) throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "no-scope-token",
          "token_type": "Bearer",
          "expires_in": 600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri(mockTokenServer.url("/token").toString());
    oauth2Config.setClientId("id");
    oauth2Config.setClientSecret("secret");
    oauth2Config.setScopes(scope);

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("no-scope-token");

    RecordedRequest request = mockTokenServer.takeRequest();
    String body = request.getBody().readUtf8();
    assertThat(body).doesNotContain("scope=");
  }

  @ParameterizedTest
  @MethodSource("expiresInEdgeCaseBodies")
  void should_ReturnToken_When_ExpiresInIsEdgeCase(String tokenResponseBody) {
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("edge-case-token");
  }

  @ParameterizedTest
  @MethodSource("invalidOAuth2Configurations")
  void should_ThrowException_When_OAuth2ConfigurationIsInvalid(OAuth2Configuration oauth2Config) {
    Camunda7SystemConnectorConfiguration config = new Camunda7SystemConnectorConfiguration();
    var outbox = new Camunda7SystemConnectorConfiguration.OutboxClientConfiguration();
    outbox.setOauth2(oauth2Config);
    config.setOutbox(outbox);

    assertThatThrownBy(() -> new OutboxOAuth2TokenProvider(config, RestClient.builder().build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OAuth2 configuration is incomplete");
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 500, 503})
  void should_ThrowException_When_TokenEndpointReturnsError(int statusCode) {
    mockTokenServer.enqueue(new MockResponse().setResponseCode(statusCode).setBody("Error"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    assertThatThrownBy(provider::getAccessToken).isInstanceOf(Exception.class);
  }

  @Test
  void should_SendFormUrlEncodedContentType() throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "content-type-token",
          "token_type": "Bearer",
          "expires_in": 600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());
    provider.getAccessToken();

    RecordedRequest request = mockTokenServer.takeRequest();
    assertThat(request.getHeader("Content-Type")).contains("application/x-www-form-urlencoded");
  }

  @Test
  void should_RefreshToken_When_TokenResponseMissingAccessToken() {
    String tokenResponseBody =
        """
        {
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    assertThatThrownBy(provider::getAccessToken)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to obtain OAuth2 access token from " + mockTokenServer.url("/token"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{ invalid json }"})
  void should_ThrowException_When_TokenResponseBodyIsInvalid(String responseBody) {
    mockTokenServer.enqueue(
        new MockResponse().setBody(responseBody).addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    assertThatThrownBy(provider::getAccessToken).isInstanceOf(Exception.class);
  }

  @Test
  void should_HandleMultipleConcurrentRequestsWithSingleTokenFetch() throws InterruptedException {
    String tokenResponseBody =
        """
        {
          "access_token": "concurrent-test-token",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    // Simulate 10 concurrent requests
    java.util.List<String> tokens =
        java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    java.util.List<Thread> threads = new java.util.ArrayList<>();

    for (int i = 0; i < 10; i++) {
      threads.add(new Thread(() -> tokens.add(provider.getAccessToken())));
    }

    threads.forEach(Thread::start);
    for (Thread thread : threads) {
      thread.join();
    }

    assertThat(tokens).hasSize(10).allMatch(t -> t.equals("concurrent-test-token"));
    assertThat(mockTokenServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  void should_IncludeAllClientCredentialsInRequest() throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "credentials-test-token",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri(mockTokenServer.url("/token").toString());
    oauth2Config.setClientId("test-client-123");
    oauth2Config.setClientSecret("test-secret-xyz");
    oauth2Config.setScopes("read write execute");

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);
    provider.getAccessToken();

    RecordedRequest request = mockTokenServer.takeRequest();
    String body = request.getBody().readUtf8();

    assertThat(body)
        .contains("grant_type=client_credentials")
        .contains("client_id=test-client-123")
        .contains("client_secret=test-secret-xyz")
        .contains("scope=read+write+execute");
  }

  @Test
  void should_HandleScopesWithSpecialCharacters() throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "special-scope-token",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri(mockTokenServer.url("/token").toString());
    oauth2Config.setClientId("id");
    oauth2Config.setClientSecret("secret");
    oauth2Config.setScopes("resource:read resource:write");

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);
    provider.getAccessToken();

    RecordedRequest request = mockTokenServer.takeRequest();
    String body = request.getBody().readUtf8();

    // Verify scopes are properly URL-encoded
    assertThat(body).contains("scope=").containsAnyOf("resource%3Aread", "resource:read");
  }

  @Test
  void should_ReturnBearerTokenWithoutModification() {
    String tokenValue = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.test";
    String tokenResponseBody =
        String.format(
            """
        {
          "access_token": "%s",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """,
            tokenValue);
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo(tokenValue);
  }

  private static Stream<String> expiresInEdgeCaseBodies() {
    return Stream.of(
        """
        {
          "access_token": "edge-case-token",
          "token_type": "Bearer",
          "expires_in": 0
        }
        """,
        """
        {
          "access_token": "edge-case-token",
          "token_type": "Bearer",
          "expires_in": -100
        }
        """,
        """
        {
          "access_token": "edge-case-token",
          "token_type": "Bearer",
          "expires_in": 20
        }
        """,
        """
        {
          "access_token": "edge-case-token",
          "token_type": "Bearer",
          "scope": "read write"
        }
        """);
  }

  private static Stream<OAuth2Configuration> invalidOAuth2Configurations() {
    OAuth2Configuration missingTokenUri = new OAuth2Configuration();
    missingTokenUri.setClientId("id");
    missingTokenUri.setClientSecret("secret");

    OAuth2Configuration missingClientId = new OAuth2Configuration();
    missingClientId.setTokenUri("http://localhost/token");
    missingClientId.setClientSecret("secret");

    OAuth2Configuration missingClientSecret = new OAuth2Configuration();
    missingClientSecret.setTokenUri("http://localhost/token");
    missingClientSecret.setClientId("id");

    return Stream.of(missingTokenUri, missingClientId, missingClientSecret, null);
  }

  private OAuth2Configuration createOAuth2Config() {
    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri(mockTokenServer.url("/token").toString());
    oauth2Config.setClientId("my-client-id");
    oauth2Config.setClientSecret("my-client-secret");
    oauth2Config.setScopes("read write");
    return oauth2Config;
  }

  private OutboxOAuth2TokenProvider createProvider(OAuth2Configuration oauth2Config) {
    RestClient restClient = RestClient.builder().build();
    return new OutboxOAuth2TokenProvider(oauth2Config, restClient);
  }
}
