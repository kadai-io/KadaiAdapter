package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration.OAuth2Configuration;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    assertThat(body).contains("grant_type=client_credentials");
    assertThat(body).contains("client_id=my-client-id");
    assertThat(body).contains("client_secret=my-client-secret");
    assertThat(body).contains("scope=read+write");
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

  @Test
  void should_NotIncludeScope_When_ScopeIsNull() throws Exception {
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
    oauth2Config.setScopes(null);

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("no-scope-token");

    RecordedRequest request = mockTokenServer.takeRequest();
    String body = request.getBody().readUtf8();
    assertThat(body).doesNotContain("scope=");
  }

  @Test
  void should_NotIncludeScope_When_ScopeIsEmpty() throws Exception {
    String tokenResponseBody =
        """
        {
          "access_token": "empty-scope-token",
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
    oauth2Config.setScopes("");

    OutboxOAuth2TokenProvider provider = createProvider(oauth2Config);
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("empty-scope-token");

    RecordedRequest request = mockTokenServer.takeRequest();
    String body = request.getBody().readUtf8();
    assertThat(body).doesNotContain("scope=");
  }

  @Test
  void should_UseDefaultExpiresIn_When_ExpiresInIsZero() {
    String tokenResponseBody =
        """
        {
          "access_token": "default-expiry-token",
          "token_type": "Bearer",
          "expires_in": 0
        }
        """;
    mockTokenServer.enqueue(
        new MockResponse()
            .setBody(tokenResponseBody)
            .addHeader("Content-Type", "application/json"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());
    String token = provider.getAccessToken();

    assertThat(token).isEqualTo("default-expiry-token");
  }

  @Test
  void should_ThrowException_When_TokenEndpointReturnsError() {
    mockTokenServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    OutboxOAuth2TokenProvider provider = createProvider(createOAuth2Config());

    assertThatThrownBy(provider::getAccessToken).isInstanceOf(Exception.class);
  }

  @Test
  void should_ThrowException_When_TokenUriIsMissing() {
    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setClientId("id");
    oauth2Config.setClientSecret("secret");

    Camunda7SystemConnectorConfiguration config = new Camunda7SystemConnectorConfiguration();
    var outbox = new Camunda7SystemConnectorConfiguration.OutboxClientConfiguration();
    outbox.setOauth2(oauth2Config);
    config.setOutbox(outbox);

    assertThatThrownBy(() -> new OutboxOAuth2TokenProvider(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OAuth2 configuration is incomplete");
  }

  @Test
  void should_ThrowException_When_ClientIdIsMissing() {
    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri("http://localhost/token");
    oauth2Config.setClientSecret("secret");

    Camunda7SystemConnectorConfiguration config = new Camunda7SystemConnectorConfiguration();
    var outbox = new Camunda7SystemConnectorConfiguration.OutboxClientConfiguration();
    outbox.setOauth2(oauth2Config);
    config.setOutbox(outbox);

    assertThatThrownBy(() -> new OutboxOAuth2TokenProvider(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OAuth2 configuration is incomplete");
  }

  @Test
  void should_ThrowException_When_ClientSecretIsMissing() {
    OAuth2Configuration oauth2Config = new OAuth2Configuration();
    oauth2Config.setTokenUri("http://localhost/token");
    oauth2Config.setClientId("id");

    Camunda7SystemConnectorConfiguration config = new Camunda7SystemConnectorConfiguration();
    var outbox = new Camunda7SystemConnectorConfiguration.OutboxClientConfiguration();
    outbox.setOauth2(oauth2Config);
    config.setOutbox(outbox);

    assertThatThrownBy(() -> new OutboxOAuth2TokenProvider(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OAuth2 configuration is incomplete");
  }

  @Test
  void should_ThrowException_When_OAuth2ConfigIsNull() {
    Camunda7SystemConnectorConfiguration config = new Camunda7SystemConnectorConfiguration();
    var outbox = new Camunda7SystemConnectorConfiguration.OutboxClientConfiguration();
    outbox.setOauth2(null);
    config.setOutbox(outbox);

    assertThatThrownBy(() -> new OutboxOAuth2TokenProvider(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OAuth2 configuration is incomplete");
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
