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

package io.kadai.adapter.configuration;

import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.ReferencedTaskClaimer;
import io.kadai.adapter.impl.ReferencedTaskCompleter;
import io.kadai.adapter.manager.AdapterManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configures the adapter. */
@EnableScheduling
@Import({SchedulerConfiguration.class})
@Configuration
@EnableTransactionManagement
public class AdapterConfiguration {

  @Value("${.jndi-name:no-jndi-configured}")
  private String jndiName;

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public AdapterManager manager() {
    return new AdapterManager();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskCompleter referencedTaskCompleter() {
    return new ReferencedTaskCompleter();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskClaimer referencedTaskClaimer() {
    return new ReferencedTaskClaimer();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskClaimCanceler referencedTaskClaimCanceler() {
    return new ReferencedTaskClaimCanceler();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public KadaiTaskStarter kadaiTaskStarter() {
    return new KadaiTaskStarter();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  KadaiTaskTerminator kadaiTaskTerminator() {
    return new KadaiTaskTerminator();
  }
}
