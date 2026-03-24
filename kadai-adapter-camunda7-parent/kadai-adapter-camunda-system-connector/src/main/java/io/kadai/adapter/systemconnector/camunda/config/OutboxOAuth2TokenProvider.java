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

package io.kadai.adapter.systemconnector.camunda.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration.OAuth2Configuration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Provides OAuth2 access tokens for communication with the Camunda 7 Outbox REST API. This
 * component is only activated when the outbox auth-type is configured as OAUTH2.
 *
 * <p>Uses a lightweight RestClient-based approach to fetch tokens from the configured token
 * endpoint using the OAuth2 client-credentials grant. Tokens are cached and automatically refreshed
 * when they expire.
 */
@Component
@ConditionalOnProperty(
    name = "kadai-adapter.plugin.camunda7.outbox.auth-type",
    havingValue = "OAUTH2")
public class OutboxOAuth2TokenProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxOAuth2TokenProvider.class);
  private static final long EXPIRY_BUFFER_SECONDS = 30;

  private final OAuth2Configuration oauth2Config;
  private final RestClient tokenRestClient;

  private String cachedAccessToken;
  private Instant tokenExpiresAt;

  @Autowired
  public OutboxOAuth2TokenProvider(Camunda7SystemConnectorConfiguration config) {
    this.oauth2Config = config.getOutbox().getOauth2();
    if (oauth2Config == null
        || oauth2Config.getTokenUri() == null
        || oauth2Config.getClientId() == null
        || oauth2Config.getClientSecret() == null) {
      throw new IllegalStateException(
          "OAuth2 configuration is incomplete for outbox. "
              + "Please set kadai-adapter.plugin.camunda7.outbox.oauth2.token-uri, "
              + "client-id, and client-secret.");
    }
    this.tokenRestClient = RestClient.builder().build();
  }

  // visible for testing
  public OutboxOAuth2TokenProvider(OAuth2Configuration oauth2Config, RestClient tokenRestClient) {
    this.oauth2Config = oauth2Config;
    this.tokenRestClient = tokenRestClient;
  }

  /**
   * Returns a valid OAuth2 access token. If the current token is expired or about to expire, a new
   * token is fetched from the authorization server.
   *
   * @return a valid Bearer access token
   */
  public synchronized String getAccessToken() {
    if (cachedAccessToken == null || isTokenExpired()) {
      refreshToken();
    }
    return cachedAccessToken;
  }

  private boolean isTokenExpired() {
    return tokenExpiresAt == null
        || Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isAfter(tokenExpiresAt);
  }

  private void refreshToken() {
    LOGGER.debug("Fetching new OAuth2 token from {}", oauth2Config.getTokenUri());

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "client_credentials");
    formData.add("client_id", oauth2Config.getClientId());
    formData.add("client_secret", oauth2Config.getClientSecret());
    if (oauth2Config.getScopes() != null && !oauth2Config.getScopes().isEmpty()) {
      formData.add("scope", oauth2Config.getScopes());
    }

    TokenResponse response =
        tokenRestClient
            .post()
            .uri(oauth2Config.getTokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(TokenResponse.class);

    if (response == null || response.accessToken == null) {
      throw new IllegalStateException(
          "Failed to obtain OAuth2 access token from " + oauth2Config.getTokenUri());
    }

    this.cachedAccessToken = response.accessToken;
    long expiresIn = response.expiresIn > 0 ? response.expiresIn : 300;
    this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

    LOGGER.debug("Successfully obtained OAuth2 token, expires in {} seconds", expiresIn);
  }

  /** DTO for the OAuth2 token endpoint response. */
  static class TokenResponse {

    @JsonProperty("access_token")
    String accessToken;

    @JsonProperty("token_type")
    String tokenType;

    @JsonProperty("expires_in")
    long expiresIn;

    @JsonProperty("scope")
    String scope;
  }
}
