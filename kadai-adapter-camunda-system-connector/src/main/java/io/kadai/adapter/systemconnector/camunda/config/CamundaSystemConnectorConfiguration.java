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
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimCanceler;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimer;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskEventCleaner;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
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
public class CamundaSystemConnectorConfiguration {

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
  CamundaSystemUrls camundaSystemUrls(
      @Value("${kadai-system-connector-camundaSystemURLs}") final String strUrls) {
    return new CamundaSystemUrls(strUrls);
  }

  @Bean
  Duration getLockDuration(
      @Value("${kadai.adapter.events.lockDuration:#{0}}") final Long lockDuration) {
    return Duration.ofSeconds(lockDuration);
  }

  @Bean
  Integer getFromKadaiToAdapterBatchSize(
      @Value("${kadai.adapter.sync.kadai.batchSize:#{64}}") final Integer batchSize) {
    return batchSize;
  }

  @Bean
  @DependsOn(value = {"httpHeaderProvider"})
  CamundaTaskRetriever camundaTaskRetriever(
      final HttpHeaderProvider httpHeaderProvider,
      final ObjectMapper objectMapper,
      final RestClient restClient) {
    return new CamundaTaskRetriever(httpHeaderProvider, objectMapper, restClient);
  }

  @Bean
  CamundaTaskCompleter camundaTaskCompleter(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new CamundaTaskCompleter(httpHeaderProvider, restClient);
  }

  @Bean
  CamundaTaskClaimer camundaTaskClaimer(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new CamundaTaskClaimer(httpHeaderProvider, restClient);
  }

  @Bean
  CamundaTaskClaimCanceler camundaTaskClaimCanceler(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new CamundaTaskClaimCanceler(httpHeaderProvider, restClient);
  }

  @Bean
  CamundaTaskEventCleaner camundaTaskEventCleaner(
      final HttpHeaderProvider httpHeaderProvider, final RestClient restClient) {
    return new CamundaTaskEventCleaner(httpHeaderProvider, restClient);
  }
}
