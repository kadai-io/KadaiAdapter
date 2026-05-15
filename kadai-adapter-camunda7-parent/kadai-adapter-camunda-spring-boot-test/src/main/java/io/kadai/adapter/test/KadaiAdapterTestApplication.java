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

package io.kadai.adapter.test;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.test.configuration.Camunda7Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Application to test the integration of an externally hosted Camunda 7 BPM engine (started in a
 * Docker container by {@code Camunda7TestcontainersConfiguration}) with the KADAI Adapter. The
 * Camunda engine no longer runs in this JVM; this application only hosts the kadai-adapter
 * scheduler and the kadai-outbox REST API.
 */
@EnableScheduling
@ComponentScan("io.kadai.adapter")
@Import({AdapterConfiguration.class, Camunda7Configuration.class})
@SpringBootApplication
@EnableTransactionManagement
public class KadaiAdapterTestApplication {

  public static void main(String... args) {
    SpringApplication.run(KadaiAdapterTestApplication.class, args);
  }
}
