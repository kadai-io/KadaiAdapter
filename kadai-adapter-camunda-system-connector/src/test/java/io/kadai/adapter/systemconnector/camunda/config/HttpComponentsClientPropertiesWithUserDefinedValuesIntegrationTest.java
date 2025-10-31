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

import io.kadai.adapter.systemconnector.camunda.config.HttpComponentsClientPropertiesWithUserDefinedValuesIntegrationTest.OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration;
import io.kadai.adapter.util.config.HttpComponentsClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration.class},
    properties = {
      "httpcomponentsclient.connection-timeout=1000",
      "httpcomponentsclient.read-timeout=10000"
    })
class HttpComponentsClientPropertiesWithUserDefinedValuesIntegrationTest {

  @Test
  void should_HaveConnectionTimeout1000ms_When_PropertyOkHttpConnectionTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getConnectionTimeout()).isEqualTo(1_000);
  }

  @Test
  void should_HaveReadTimeout10000ms_When_PropertyOkHttpReadTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getReadTimeout()).isEqualTo(10_000);
  }

  @EnableConfigurationProperties(HttpComponentsClientProperties.class)
  static class OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration {}
}
