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
 */

package io.kadai.adapter.camunda.autoconfigure;

import io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers the {@link KadaiParseListenerProcessEnginePlugin} as a Spring
 * bean. When this jar is dropped into a Camunda Spring Boot application (e.g. Camunda BPM Run's
 * {@code userlib/} directory), the Camunda Spring Boot starter automatically picks up {@link
 * ProcessEnginePlugin} beans and registers them with the engine.
 */
@AutoConfiguration
@ConditionalOnClass(ProcessEnginePlugin.class)
public class KadaiListenerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(KadaiParseListenerProcessEnginePlugin.class)
  public KadaiParseListenerProcessEnginePlugin kadaiParseListenerProcessEnginePlugin() {
    return new KadaiParseListenerProcessEnginePlugin();
  }
}
