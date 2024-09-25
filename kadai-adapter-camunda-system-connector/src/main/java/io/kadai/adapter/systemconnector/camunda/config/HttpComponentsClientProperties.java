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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "httpcomponentsclient")
public class HttpComponentsClientProperties {

  private long connectionTimeout = 2_000;

  private long readTimeout = 5_000;

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public long getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(long readTimeout) {
    this.readTimeout = readTimeout;
  }
}
