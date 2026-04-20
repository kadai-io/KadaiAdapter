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

package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration.AuthType;
import io.kadai.adapter.systemconnector.camunda.config.OutboxOAuth2TokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderProvider {

  private static final String UNDEFINED = "undefined";

  private final Camunda7SystemConnectorConfiguration camunda7config;
  private final Optional<OutboxOAuth2TokenProvider> oauth2TokenProvider;

  public HttpHeaderProvider(
      Camunda7SystemConnectorConfiguration camunda7config,
      Optional<OutboxOAuth2TokenProvider> oauth2TokenProvider) {
    this.camunda7config = camunda7config;
    this.oauth2TokenProvider = oauth2TokenProvider;
  }

  public HttpHeaders camunda7RestApiHeaders() {
    if (UNDEFINED.equals(camunda7config.getClient().getUsername())) {
      return new HttpHeaders();
    } else {
      String plainCreds =
          camunda7config.getClient().getUsername() + ":" + camunda7config.getClient().getPassword();
      return encodeBasicAuthHeaders(plainCreds);
    }
  }

  public HttpHeaders outboxRestApiHeaders() {
    AuthType authType = camunda7config.getOutbox().getAuthType();

    if (authType == AuthType.OAUTH2) {
      return oauth2BearerHeaders();
    }

    if (camunda7config.getOutbox().getClient() == null
        || UNDEFINED.equals(camunda7config.getOutbox().getClient().getUsername())) {
      return new HttpHeaders();
    } else {
      String plainCreds =
          camunda7config.getOutbox().getClient().getUsername()
              + ":"
              + camunda7config.getOutbox().getClient().getPassword();
      return encodeBasicAuthHeaders(plainCreds);
    }
  }

  public HttpHeaders getHttpHeadersForCamunda7RestApi() {
    HttpHeaders headers = camunda7RestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  public HttpHeaders getHttpHeadersForOutboxRestApi() {
    HttpHeaders headers = outboxRestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders oauth2BearerHeaders() {
    OutboxOAuth2TokenProvider provider =
        oauth2TokenProvider.orElseThrow(
            () ->
                new IllegalStateException(
                    "OAuth2 auth-type is configured for the outbox, "
                        + "but no OutboxOAuth2TokenProvider bean is available. "
                        + "Please check your OAuth2 configuration."));
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + provider.getAccessToken());
    addXsrfHeaders(headers);
    return headers;
  }

  private HttpHeaders encodeBasicAuthHeaders(String credentials) {
    byte[] credentialsBytes = credentials.getBytes(StandardCharsets.US_ASCII);
    String encodedCredentials = Base64.getEncoder().encodeToString(credentialsBytes);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    addXsrfHeaders(headers);
    return headers;
  }

  private void addXsrfHeaders(HttpHeaders headers) {
    if (camunda7config.getXsrfToken() != null && !camunda7config.getXsrfToken().isEmpty()) {
      headers.add("Cookie", "XSRF-TOKEN=" + camunda7config.getXsrfToken());
      headers.add("X-XSRF-TOKEN", camunda7config.getXsrfToken());
    }
  }
}
