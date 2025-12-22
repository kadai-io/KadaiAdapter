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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskClaimCanceler;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskClaimer;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskCompleter;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskEventCleaner;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7TaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.util.config.HttpComponentsClientProperties;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configures the camunda system connector. */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
public class Camunda7SystemConnectorConfiguration {

  @Bean
  RestClient restClient(HttpComponentsClientProperties httpComponentsClientProperties) {
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    requestFactory.setConnectTimeout(
        (int) Duration.ofMillis(httpComponentsClientProperties.getConnectionTimeout()).toMillis());
    requestFactory.setReadTimeout(
        (int) Duration.ofMillis(httpComponentsClientProperties.getReadTimeout()).toMillis());

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Bean
  HttpHeaderProvider httpHeaderProvider() {
    return new HttpHeaderProvider();
  }

  @Bean
  Camunda7SystemUrls camunda7SystemUrls(
      @Value("${kadai-system-connector-camunda7SystemURLs}") final String strUrls) {
    return new Camunda7SystemUrls(strUrls);
  }

  @Bean
  Duration getLockDuration(
      @Value("${kadai.adapter.events.lockDuration:#{0}}") final Long lockDuration) {
    return Duration.ofSeconds(lockDuration);
  }

  @Bean
  @DependsOn(value = {"httpHeaderProvider"})
  Camunda7TaskRetriever camunda7TaskRetriever(
      final HttpHeaderProvider httpHeaderProvider,
      final ObjectMapper objectMapper,
      final RestClient restClient) {
    return new Camunda7TaskRetriever(httpHeaderProvider, objectMapper, restClient);
  }

  @Bean
  Camunda7TaskCompleter camunda7TaskCompleter(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new Camunda7TaskCompleter(httpHeaderProvider, restClient);
  }

  @Bean
  Camunda7TaskClaimer camunda7TaskClaimer(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new Camunda7TaskClaimer(httpHeaderProvider, restClient);
  }

  @Bean
  Camunda7TaskClaimCanceler camunda7TaskClaimCanceler(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new Camunda7TaskClaimCanceler(httpHeaderProvider, restClient);
  }

  @Bean
  Camunda7TaskEventCleaner camunda7TaskEventCleaner(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new Camunda7TaskEventCleaner(httpHeaderProvider, restClient);
  }
}
