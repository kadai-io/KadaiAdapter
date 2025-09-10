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

import io.kadai.adapter.systemconnector.camunda.config.Camunda7Systems.Camunda7System;
import java.util.Iterator;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@ConfigurationProperties(prefix = "kadai.adapter.camunda7")
public class Camunda7Systems implements Iterable<Camunda7System> {

  private List<Camunda7System> systems;

  public List<Camunda7System> getSystems() {
    return systems;
  }

  public void setSystems(List<Camunda7System> systems) {
    this.systems = systems;
  }

  @Override
  public @NonNull Iterator<Camunda7System> iterator() {
    return systems.iterator();
  }

  public static class Camunda7System {
    private String id;
    private String systemUrl;
    private String outboxUrl;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getSystemUrl() {
      return systemUrl;
    }

    public void setSystemUrl(String systemUrl) {
      this.systemUrl = systemUrl;
    }

    public String getOutboxUrl() {
      return outboxUrl;
    }

    public void setOutboxUrl(String outboxUrl) {
      this.outboxUrl = outboxUrl;
    }
  }
}
