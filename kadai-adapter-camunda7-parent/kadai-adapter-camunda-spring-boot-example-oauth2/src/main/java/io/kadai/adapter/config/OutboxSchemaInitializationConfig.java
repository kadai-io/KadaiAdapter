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

package io.kadai.adapter.config;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxDataSource;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Initializes the schema used by the embedded real Outbox REST service. */
@Configuration
public class OutboxSchemaInitializationConfig {

  /**
   * Initializes the real outbox datasource on startup so the embedded outbox REST service can be
   * used immediately in the runnable example and its integration tests.
   *
   * @return runner that creates the outbox schema and tables if missing
   */
  @Bean
  public ApplicationRunner initializeOutboxSchema() {
    return args -> {
      DataSource outboxDataSource = OutboxDataSource.get();
      ResourceDatabasePopulator populator =
          new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
      populator.execute(outboxDataSource);
    };
  }
}

