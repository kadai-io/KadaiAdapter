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

import io.kadai.adapter.util.config.HttpComponentsClientProperties;
import java.util.List;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configures the camunda system connector. */
@Configuration
@ConfigurationProperties("kadai-adapter.plugin.camunda7")
@DependsOn(value = {"adapterSpringContextProvider"})
public class Camunda7SystemConnectorConfiguration {

  /** List of Camunda 7 systems to connect with the KadaiAdapter. */
  private List<Camunda7System> systems;

  /** Configuration for the Camunda 7 Client. */
  private ClientConfiguration client = new ClientConfiguration();

  /** Configuration for the Camunda 7 Outbox Client. */
  private OutboxClientConfiguration outbox = new OutboxClientConfiguration();

  /** Configuration for the Camunda 7 Claiming-Behavior. */
  private ClaimingConfiguration claiming = new ClaimingConfiguration();

  /**
   * Duration in seconds used for locking tasks in the Outbox in clustered
   * KadaiAdapter-Environments.
   */
  private Long lockDuration = 0L;

  /** XSRF-Token used when communicating with either Camunda or the Outbox REST-API. */
  private String xsrfToken;

  public List<Camunda7System> getSystems() {
    return systems;
  }

  public void setSystems(List<Camunda7System> systems) {
    this.systems = systems;
  }

  public ClientConfiguration getClient() {
    return client;
  }

  public void setClient(ClientConfiguration client) {
    this.client = client;
  }

  public OutboxClientConfiguration getOutbox() {
    return outbox;
  }

  public void setOutbox(OutboxClientConfiguration outbox) {
    this.outbox = outbox;
  }

  public ClaimingConfiguration getClaiming() {
    return claiming;
  }

  public void setClaiming(ClaimingConfiguration claiming) {
    this.claiming = claiming;
  }

  public Long getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(Long lockDuration) {
    this.lockDuration = lockDuration;
  }

  public String getXsrfToken() {
    return xsrfToken;
  }

  public void setXsrfToken(String xsrfToken) {
    this.xsrfToken = xsrfToken;
  }

  @Bean
  RestClient restClient(HttpComponentsClientProperties props) {
    ConnectionConfig connectionConfig =
        ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(props.getConnectionTimeout()))
            .build();

    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .build();

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(props.getReadTimeout()))
            .build();

    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Bean
  List<Camunda7System> camunda7Systems() {
    return this.getSystems();
  }

  /** Supported authentication types for the Outbox REST API. */
  public enum AuthType {
    BASIC,
    OAUTH2
  }

  public static class ClientConfiguration {

    /** Basic-Auth username. */
    private String username;

    /** Basic-Auth password. */
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static class OutboxClientConfiguration {

    /** The authentication type to use: BASIC (default) or OAUTH2. */
    private AuthType authType = AuthType.BASIC;

    /** Configuration for the Camunda 7 Outbox Client (used when authType is BASIC). */
    private ClientConfiguration client;

    /** Configuration for OAuth2 authentication (used when authType is OAUTH2). */
    private OAuth2Configuration oauth2;

    public AuthType getAuthType() {
      return authType;
    }

    public void setAuthType(AuthType authType) {
      this.authType = authType;
    }

    public ClientConfiguration getClient() {
      return client;
    }

    public void setClient(ClientConfiguration client) {
      this.client = client;
    }

    public OAuth2Configuration getOauth2() {
      return oauth2;
    }

    public void setOauth2(OAuth2Configuration oauth2) {
      this.oauth2 = oauth2;
    }
  }

  public static class OAuth2Configuration {

    /** The token endpoint URI of the OAuth2 authorization server. */
    private String tokenUri;

    /** The OAuth2 client ID. */
    private String clientId;

    /** The OAuth2 client secret. */
    private String clientSecret;

    /** Comma-separated list of scopes to request. */
    private String scopes;

    public String getTokenUri() {
      return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
      this.tokenUri = tokenUri;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public String getScopes() {
      return scopes;
    }

    public void setScopes(String scopes) {
      this.scopes = scopes;
    }
  }

  public static class ClaimingConfiguration {
    /**
     * Flag for enabling or disabling Claiming as part of the KadaiAdapter synchronization with
     * Camunda 7.
     */
    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
