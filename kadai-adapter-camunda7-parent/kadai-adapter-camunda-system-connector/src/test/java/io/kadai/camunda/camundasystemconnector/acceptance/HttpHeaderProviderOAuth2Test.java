package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest
class HttpHeaderProviderOAuth2Test {

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        AdapterSpringContextProvider.class
      })
  @TestPropertySource(
      properties = {
        "kadai-adapter.plugin.camunda7.outbox.auth-type=BASIC",
        "kadai-adapter.plugin.camunda7.outbox.client.username=outboxUser",
        "kadai-adapter.plugin.camunda7.outbox.client.password=outboxPassword"
      })
  class BasicAuthDefaultTest {

    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Autowired private Optional<OutboxOAuth2TokenProvider> oauth2TokenProvider;

    @Test
    void should_ReturnBasicAuthHeader_When_AuthTypeIsBasic() {
      HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
      assertThat(headers.getFirst("Authorization")).startsWith("Basic ");
    }

    @Test
    void should_NotCreateOAuth2TokenProvider_When_AuthTypeIsBasic() {
      assertThat(oauth2TokenProvider).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        AdapterSpringContextProvider.class
      })
  class DefaultAuthTypeTest {

    @Autowired private Camunda7SystemConnectorConfiguration config;

    @Test
    void should_DefaultToBasicAuthType() {
      assertThat(config.getOutbox().getAuthType())
          .isEqualTo(Camunda7SystemConnectorConfiguration.AuthType.BASIC);
    }
  }

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        AdapterSpringContextProvider.class
      })
  @TestPropertySource(
      properties = {
        "kadai-adapter.plugin.camunda7.outbox.auth-type=BASIC",
        "kadai-adapter.plugin.camunda7.outbox.client.username=undefined"
      })
  class BasicAuthWithUndefinedUserTest {

    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Test
    void should_ReturnEmptyHeaders_When_BasicAuthUsernameIsUndefined() {
      HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
      assertThat(headers).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        OutboxOAuth2TokenProvider.class,
        AdapterSpringContextProvider.class
      })
  @TestPropertySource(
      properties = {
        "kadai-adapter.plugin.camunda7.outbox.auth-type=OAUTH2",
        "kadai-adapter.plugin.camunda7.outbox.oauth2.token-uri=http://localhost:9999/token",
        "kadai-adapter.plugin.camunda7.outbox.oauth2.client-id=test-client",
        "kadai-adapter.plugin.camunda7.outbox.oauth2.client-secret=test-secret",
        "kadai-adapter.plugin.camunda7.outbox.oauth2.scopes=read write",
        "kadai-adapter.plugin.camunda7.xsrf-token=MY_XSRF_TOKEN"
      })
  class OAuth2AuthTypeConfigTest {

    @Autowired private Camunda7SystemConnectorConfiguration config;

    @Autowired private Optional<OutboxOAuth2TokenProvider> oauth2TokenProvider;

    @Test
    void should_SetAuthTypeToOAuth2() {
      assertThat(config.getOutbox().getAuthType())
          .isEqualTo(Camunda7SystemConnectorConfiguration.AuthType.OAUTH2);
    }

    @Test
    void should_BindOAuth2Properties() {
      assertThat(config.getOutbox().getOauth2()).isNotNull();
      assertThat(config.getOutbox().getOauth2().getTokenUri())
          .isEqualTo("http://localhost:9999/token");
      assertThat(config.getOutbox().getOauth2().getClientId()).isEqualTo("test-client");
      assertThat(config.getOutbox().getOauth2().getClientSecret()).isEqualTo("test-secret");
      assertThat(config.getOutbox().getOauth2().getScopes()).isEqualTo("read write");
    }

    @Test
    void should_CreateOAuth2TokenProviderBean() {
      assertThat(oauth2TokenProvider).isPresent();
    }
  }

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        AdapterSpringContextProvider.class
      })
  @TestPropertySource(
      properties = {
        "kadai-adapter.plugin.camunda7.outbox.auth-type=BASIC",
        "kadai-adapter.plugin.camunda7.outbox.client.username=outboxUser",
        "kadai-adapter.plugin.camunda7.outbox.client.password=outboxPassword",
        "kadai-adapter.plugin.camunda7.xsrf-token=XSRF_123"
      })
  class BasicAuthWithXsrfTest {

    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Test
    void should_IncludeXsrfHeaders_When_BasicAuthWithXsrfToken() {
      HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
      assertThat(headers.getFirst("Authorization")).startsWith("Basic ");
      assertThat(headers.get("Cookie")).containsExactly("XSRF-TOKEN=XSRF_123");
      assertThat(headers.get("X-XSRF-TOKEN")).containsExactly("XSRF_123");
    }
  }

  @Nested
  @SpringBootTest(
      classes = {
        Camunda7TaskRetriever.class,
        HttpHeaderProvider.class,
        Camunda7SystemConnectorConfiguration.class,
        AdapterSpringContextProvider.class
      })
  @TestPropertySource(
      properties = {
        "kadai-adapter.plugin.camunda7.outbox.auth-type=BASIC",
        "kadai-adapter.plugin.camunda7.outbox.client.username=outboxUser",
        "kadai-adapter.plugin.camunda7.outbox.client.password=outboxPassword"
      })
  class CamundaHeadersUnaffectedTest {

    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Test
    void should_ReturnBasicAuthForCamundaRestApi_RegardlessOfOutboxAuthType() {
      HttpHeaders headers = httpHeaderProvider.camunda7RestApiHeaders();
      // Camunda REST API uses its own client config, not the outbox config
      assertThat(headers.getFirst("Authorization")).startsWith("Basic ");
    }
  }
}
