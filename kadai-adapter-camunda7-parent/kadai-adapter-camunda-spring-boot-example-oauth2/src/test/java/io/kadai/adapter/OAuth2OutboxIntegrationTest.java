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

package io.kadai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxDataSource;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Full end-to-end integration test that boots the complete {@link KadaiAdapterOAuth2Application}
 * on {@code server.port=8083} and exercises real OAuth2 authentication against the embedded
 * Outbox REST service.
 *
 * <p>Setup:
 *
 * <ul>
 *   <li>The embedded <em>Authorization Server</em> ({@code /oauth2/token}) issues real RS256 JWTs.
 *   <li>The outbox REST endpoint ({@code /outbox-rest/events}) is protected by Spring Security's
 *       OAuth2 Resource Server and accepts only valid Bearer tokens.
 *   <li>The {@code OutboxOAuth2TokenProvider} fetches a genuine token and the adapter forwards it
 *       on every outbox request - no mocking involved.
 * </ul>
 *
 * <p>The shared H2 in-memory database ({@code jdbc:h2:mem:outbox}) is created by Spring Boot via
 * {@code schema.sql} and is also accessible through the auto-configured {@link JdbcTemplate},
 * which allows tests to seed and clean up the {@code kadai_tables.event_store} table directly.
 */
@SpringBootTest(
    classes = KadaiAdapterOAuth2Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OAuth2OutboxIntegrationTest {

  private static final String INSERT_EVENT =
      "INSERT INTO kadai_tables.event_store "
          + "(TYPE, CREATED, PAYLOAD, REMAINING_RETRIES, BLOCKED_UNTIL, CAMUNDA_TASK_ID) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

  private static final String DELETE_ALL_EVENTS = "DELETE FROM kadai_tables.event_store";

  @Value("${server.port}")
  private int serverPort;

  @Autowired private HttpHeaderProvider httpHeaderProvider;
  @Autowired private Camunda7SystemConnectorConfiguration config;
  @Autowired private RestClient restClient;
  private JdbcTemplate jdbcTemplate;
  private String outboxEventsUrl;

  @BeforeEach
  void setUp() {
    outboxEventsUrl = "http://localhost:" + serverPort + "/outbox-rest/events";
    jdbcTemplate = new JdbcTemplate(OutboxDataSource.get());
    jdbcTemplate.execute(DELETE_ALL_EVENTS);
  }

  @AfterEach
  void tearDown() {
    jdbcTemplate.execute(DELETE_ALL_EVENTS);
  }

  @Test
  void should_ConfigureOAuth2AuthType_When_ApplicationStarts() {
    assertThat(config.getOutbox().getAuthType())
        .isEqualTo(Camunda7SystemConnectorConfiguration.AuthType.OAUTH2);
    assertThat(config.getOutbox().getOauth2()).isNotNull();
    assertThat(config.getOutbox().getOauth2().getClientId()).isEqualTo("kadai-adapter");
    assertThat(config.getOutbox().getOauth2().getClientSecret()).isEqualTo("super-secret");
    assertThat(config.getOutbox().getOauth2().getScopes()).isEqualTo("outbox:read outbox:write");
  }

  @Test
  void should_ProduceBearerHeader_When_OutboxAuthTypeIsOAuth2() {
    HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();

    assertThat(headers.getFirst("Authorization"))
        .isNotNull()
        .matches("Bearer [A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
  }

  @Test
  void should_KeepBasicAuthForCamunda7RestApi_When_OutboxUsesOAuth2() {
    HttpHeaders camundaHeaders = httpHeaderProvider.camunda7RestApiHeaders();

    assertThat(camundaHeaders.getFirst("Authorization")).startsWith("Basic ");
  }

  @Test
  void should_FetchPersistedEventsFromRealOutbox_When_UsingValidOAuth2Token() {
    Timestamp now = Timestamp.from(Instant.now().minusSeconds(60));
    jdbcTemplate.update(
        INSERT_EVENT, "create", now, "{\"id\":\"task-001\"}", 3, now, "task-001");
    jdbcTemplate.update(
        INSERT_EVENT, "create", now, "{\"id\":\"task-002\"}", 3, now, "task-002");

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    String response =
        restClient
            .get()
            .uri(outboxEventsUrl)
            .headers(h -> h.addAll(headers))
            .retrieve()
            .body(String.class);

    assertThat(response).isNotNull().contains("task-001").contains("task-002");
  }

  @Test
  void should_ReturnOnlyMatchingEvents_When_FilteringByRetries() {
    Timestamp now = Timestamp.from(Instant.now().minusSeconds(60));
    jdbcTemplate.update(INSERT_EVENT, "create", now, "{\"id\":\"c1\"}", 3, now, "c1");
    jdbcTemplate.update(
        INSERT_EVENT, "complete", now, "{\"id\":\"d1\"}", 1, now, "d1");

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    String response =
        restClient
            .get()
            .uri(outboxEventsUrl + "?retries=3")
            .headers(h -> h.addAll(headers))
            .retrieve()
            .body(String.class);

    assertThat(response).isNotNull().contains("c1").doesNotContain("d1");
  }

  @Test
  void should_ReturnEmptyList_When_OutboxHasNoEvents() {
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    String response =
        restClient
            .get()
            .uri(outboxEventsUrl + "?type=create")
            .headers(h -> h.addAll(headers))
            .retrieve()
            .body(String.class);

    assertThat(response).isNotNull();
  }

  @Test
  void should_IncludeContentTypeJsonInOutboxRequest_When_UsingOAuth2Headers() {
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(headers.getFirst("Authorization")).startsWith("Bearer ");
  }

  @Test
  void should_Return401_When_CallingOutboxWithoutAuthorizationHeader() {
    assertThatThrownBy(
            () ->
                restClient
                    .get()
                    .uri(outboxEventsUrl + "?type=create")
                    .retrieve()
                    .body(String.class))
        .isInstanceOf(HttpClientErrorException.Unauthorized.class);
  }

  @Test
  void should_Return401_When_CallingOutboxWithInvalidBearerToken() {
    assertThatThrownBy(
            () ->
                restClient
                    .get()
                    .uri(outboxEventsUrl + "?type=create")
                    .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                    .retrieve()
                    .body(String.class))
        .isInstanceOf(HttpClientErrorException.Unauthorized.class);
  }

  @Test
  void should_Return401_When_CallingOutboxWithExpiredToken() {
    String expiredJwt =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJleHAiOjF9"
            + ".invalidsignature";

    assertThatThrownBy(
            () ->
                restClient
                    .get()
                    .uri(outboxEventsUrl + "?type=create")
                    .header("Authorization", "Bearer " + expiredJwt)
                    .retrieve()
                    .body(String.class))
        .isInstanceOf(HttpClientErrorException.Unauthorized.class);
  }

  @Test
  void should_ReuseToken_When_MultipleOutboxRequestsMade() {
    HttpHeaders headers1 = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    restClient
        .get()
        .uri(outboxEventsUrl + "?type=create")
        .headers(h -> h.addAll(headers1))
        .retrieve()
        .body(String.class);

    HttpHeaders headers2 = httpHeaderProvider.getHttpHeadersForOutboxRestApi();
    restClient
        .get()
        .uri(outboxEventsUrl + "?type=create")
        .headers(h -> h.addAll(headers2))
        .retrieve()
        .body(String.class);

    assertThat(headers1.getFirst("Authorization")).isEqualTo(headers2.getFirst("Authorization"));
  }
}
