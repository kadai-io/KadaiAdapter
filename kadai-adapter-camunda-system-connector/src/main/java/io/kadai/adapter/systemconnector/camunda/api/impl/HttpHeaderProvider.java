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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderProvider {

  private static final String UNDEFINED = "undefined";

  @Value("${kadai-system-connector-camunda-rest-api-user-name:undefined}")
  private String camundaRestApiUserName;

  @Value("${kadai-system-connector-camunda-rest-api-user-password:undefined}")
  private String camundaRestApiUserPassword;

  @Value("${kadai-system-connector-outbox-rest-api-user-name:undefined}")
  private String outboxRestApiUserName;

  @Value("${kadai-system-connector-outbox-rest-api-user-password:undefined}")
  private String outboxRestApiUserPassword;

  @Value("${kadai.adapter.xsrf.token:}")
  private String xsrfToken;

  public HttpHeaders camundaRestApiHeaders() {
    if (UNDEFINED.equals(camundaRestApiUserName)) {
      return new HttpHeaders();
    } else {
      String plainCreds = camundaRestApiUserName + ":" + camundaRestApiUserPassword;
      return encodeHttpHeaders(plainCreds);
    }
  }

  public HttpHeaders outboxRestApiHeaders() {
    if (UNDEFINED.equals(outboxRestApiUserName)) {
      return new HttpHeaders();
    } else {
      String plainCreds = outboxRestApiUserName + ":" + outboxRestApiUserPassword;
      return encodeHttpHeaders(plainCreds);
    }
  }

  public HttpHeaders getHttpHeadersForCamundaRestApi() {
    HttpHeaders headers = camundaRestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  public HttpHeaders getHttpHeadersForOutboxRestApi() {
    HttpHeaders headers = outboxRestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders encodeHttpHeaders(String credentials) {
    byte[] credentialsBytes = credentials.getBytes(StandardCharsets.US_ASCII);
    String encodedCredentials = Base64.getEncoder().encodeToString(credentialsBytes);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    if (xsrfToken != null && !xsrfToken.isEmpty()) {
      headers.add("Cookie", "XSRF-TOKEN=" + xsrfToken);
      headers.add("X-XSRF-TOKEN", xsrfToken);
    }
    return headers;
  }
}
