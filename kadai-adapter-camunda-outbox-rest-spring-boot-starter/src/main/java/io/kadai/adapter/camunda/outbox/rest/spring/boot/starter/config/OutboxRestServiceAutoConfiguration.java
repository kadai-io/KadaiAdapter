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

import io.kadai.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import io.kadai.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import io.kadai.adapter.camunda.outbox.rest.filter.CsrfTokenIssuanceFilter;
import io.kadai.adapter.camunda.outbox.rest.filter.CsrfTokenService;
import io.kadai.adapter.camunda.outbox.rest.filter.CsrfValidationFilter;
import io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for the outbox REST service. */
@Configuration
@ConditionalOnClass(CamundaTaskEventsController.class)
public class OutboxRestServiceAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public OutboxRestServiceConfig outboxRestServiceConfig() {
    return new OutboxRestServiceConfig();
  }

  @Bean
  @ConditionalOnMissingBean
  public CamundaTaskEventsController camundaTaskEventsController() {
    return new CamundaTaskEventsController();
  }

  @Bean
  @ConditionalOnMissingBean
  public KadaiParseListenerProcessEnginePlugin kadaiParseListenerProcessEnginePlugin() {
    return new KadaiParseListenerProcessEnginePlugin();
  }

  @Bean
  @ConditionalOnMissingBean
  public CsrfTokenIssuanceFilter csrfTokenIssuanceFilter() {
    return new CsrfTokenIssuanceFilter();
  }

  @Bean
  @ConditionalOnMissingBean
  public CsrfValidationFilter csrfValidationFilter() {
    return new CsrfValidationFilter();
  }

  @Bean
  @ConditionalOnMissingBean
  public CsrfTokenService csrfTokenService() throws NoSuchAlgorithmException {
    return new CsrfTokenService();
  }
}
