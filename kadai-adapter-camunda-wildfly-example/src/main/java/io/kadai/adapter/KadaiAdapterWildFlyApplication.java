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

package io.kadai.adapter;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.kadaiconnector.config.KadaiSystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemConnectorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Example Application showing the implementation of kadai-adapter for jboss application server.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {
      "io.kadai.adapter",
      "io.kadai.adapter.configuration",
      "io.kadai",
      "io.kadai.adapter.systemconnector.camunda.config",
      "io.kadai.adapter.kadaiconnector.config"
    })
@SuppressWarnings("checkstyle:Indentation")
@Import({
  AdapterConfiguration.class,
  CamundaSystemConnectorConfiguration.class,
  KadaiSystemConnectorConfiguration.class
})
public class KadaiAdapterWildFlyApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(KadaiAdapterWildFlyApplication.class, args);
  }
}
