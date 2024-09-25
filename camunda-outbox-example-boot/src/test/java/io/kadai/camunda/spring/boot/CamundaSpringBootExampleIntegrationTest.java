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

package io.kadai.camunda.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import io.kadai.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CamundaSpringBootExample.class, webEnvironment = RANDOM_PORT)
class CamundaSpringBootExampleIntegrationTest {

  private final OutboxRestServiceConfig outboxRestServiceConfig;

  private final CamundaTaskEventsController camundaTaskEventsController;

  private final KadaiParseListenerProcessEnginePlugin kadaiParseListenerProcessEnginePlugin;

  CamundaSpringBootExampleIntegrationTest(
      @Autowired(required = false) OutboxRestServiceConfig outboxRestServiceConfig,
      @Autowired(required = false) CamundaTaskEventsController camundaTaskEventsController,
      @Autowired(required = false)
          KadaiParseListenerProcessEnginePlugin kadaiParseListenerProcessEnginePlugin) {
    this.outboxRestServiceConfig = outboxRestServiceConfig;
    this.camundaTaskEventsController = camundaTaskEventsController;
    this.kadaiParseListenerProcessEnginePlugin = kadaiParseListenerProcessEnginePlugin;
  }

  @Test
  void should_AutowireOutboxRestServiceConfig_When_ApplicationIsStarting() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void should_AutowireCamundaTaskEventsController_When_ApplicationIsStarting() {
    assertThat(camundaTaskEventsController).isNotNull();
  }

  @Test
  void should_AutowireKadaiParseListenerProcessEnginePlugin_When_ApplicationIsStarting() {
    assertThat(kadaiParseListenerProcessEnginePlugin).isNotNull();
  }
}
