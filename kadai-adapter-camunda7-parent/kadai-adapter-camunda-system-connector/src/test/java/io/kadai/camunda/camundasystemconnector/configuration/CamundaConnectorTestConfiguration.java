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

package io.kadai.camunda.camundasystemconnector.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.util.config.HttpComponentsClientProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration for test of Camunda System Connector.
 *
 * @author bbr
 */
@Configuration
@EnableConfigurationProperties(HttpComponentsClientProperties.class)
public class CamundaConnectorTestConfiguration {

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  HttpHeaderProvider httpHeaderProvider() {
    return new HttpHeaderProvider();
  }

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
}
