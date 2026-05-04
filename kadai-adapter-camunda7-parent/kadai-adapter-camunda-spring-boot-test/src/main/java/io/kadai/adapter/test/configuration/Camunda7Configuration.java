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

package io.kadai.adapter.test.configuration;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Camunda BPM datasource exposure for the test JVM.
 *
 * <p>The Camunda 7 engine is hosted in a separate Docker container (see {@code
 * Camunda7TestcontainersConfiguration}) and persists into a shared PostgreSQL container. The bean
 * declared here points at the same PostgreSQL instance and is consumed by the integration tests'
 * {@code DbCleaner} to truncate the engine schema between test classes.
 *
 * <p>The connection coordinates are bound at runtime from the {@code camunda.datasource.*}
 * properties registered by the test infrastructure via {@code @DynamicPropertySource}.
 */
@Configuration
public class Camunda7Configuration {

  @Bean(name = "camundaBpmDataSource")
  @Primary
  @ConfigurationProperties(prefix = "camunda.datasource")
  public DataSource camundaBpmDataSource() {
    return DataSourceBuilder.create().build();
  }
}
