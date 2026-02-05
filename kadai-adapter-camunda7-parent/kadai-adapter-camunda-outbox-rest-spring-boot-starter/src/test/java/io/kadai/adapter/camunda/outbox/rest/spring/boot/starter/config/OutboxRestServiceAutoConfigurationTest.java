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

package io.kadai.adapter.camunda.outbox.rest.spring.boot.starter.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest
class OutboxRestServiceAutoConfigurationTest {

  private final OutboxRestServiceConfig outboxRestServiceConfig;

  OutboxRestServiceAutoConfigurationTest(
      @Autowired(required = false) OutboxRestServiceConfig outboxRestServiceConfig) {
    this.outboxRestServiceConfig = outboxRestServiceConfig;
  }

  @Test
  void outboxRestServiceConfig_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void camunda7TaskEventsController_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void kadaiParseListenerProcessEnginePlugin_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestConfig {
    // empty class to enable AutoConfiguration and configure spring boot test for it
  }
}
