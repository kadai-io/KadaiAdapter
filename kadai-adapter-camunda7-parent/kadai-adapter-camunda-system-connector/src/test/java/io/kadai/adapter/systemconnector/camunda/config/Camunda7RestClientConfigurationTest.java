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

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.util.config.HttpComponentsClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = {
      Camunda7SystemConnectorConfiguration.class,
      HttpComponentsClientProperties.class,
      AdapterSpringContextProvider.class
    })
class Camunda7RestClientConfigurationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private RestClient restClient;

  @Autowired
  @Qualifier("camunda7HealthRestClient")
  private RestClient healthRestClient;

  @Test
  void should_ResolveDistinctOperationalAndHealthRestClients() {
    assertThat(restClient).isSameAs(applicationContext.getBean("restClient"));
    assertThat(healthRestClient)
        .isSameAs(applicationContext.getBean("camunda7HealthRestClient"));
    assertThat(restClient).isNotSameAs(healthRestClient);
  }
}
