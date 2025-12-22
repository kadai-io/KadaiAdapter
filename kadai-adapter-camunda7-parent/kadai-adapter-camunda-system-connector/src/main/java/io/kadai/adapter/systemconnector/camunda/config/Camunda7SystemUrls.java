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

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.springframework.stereotype.Component;

/** Holds the URlS (Camunda REST Api and outbox REST) of the configured camunda systems. @ */
@Component
public class Camunda7SystemUrls {

  private final Set<SystemUrlInfo> urls = new HashSet<>();

  public Camunda7SystemUrls(String strUrls) {
    if (strUrls != null) {
      StringTokenizer systemTokenizer = new StringTokenizer(strUrls, ",");
      while (systemTokenizer.hasMoreTokens()) {
        String currentSystemConfigs = systemTokenizer.nextToken().trim();
        StringTokenizer systemConfigParts = new StringTokenizer(currentSystemConfigs, "|");
        SystemUrlInfo urlInfo = new SystemUrlInfo();

        urlInfo.setSystemRestUrl(systemConfigParts.nextToken().trim());
        urlInfo.setSystemTaskEventUrl(systemConfigParts.nextToken().trim());

        if (systemConfigParts.countTokens() == 1) {
          urlInfo.setCamunda7EngineIdentifier(systemConfigParts.nextToken().trim());
        }

        urls.add(urlInfo);
      }
    }
  }

  public Set<SystemUrlInfo> getUrls() {
    return urls;
  }

  @Override
  public String toString() {
    return "Camunda7SystemUrls [camunda7SystemUrls=" + urls + "]";
  }

  /**
   * Holds the URS (Camunda REST Api and outbox REST) of a specific camunda system as well as an
   * optional identfier for the responsible camunda engine.
   */
  public static class SystemUrlInfo {

    private String systemRestUrl;
    private String systemTaskEventUrl;
    private String camunda7EngineIdentifier;

    public String getSystemRestUrl() {
      return systemRestUrl;
    }

    public void setSystemRestUrl(String systemRestUrl) {
      this.systemRestUrl = systemRestUrl;
    }

    public String getSystemTaskEventUrl() {
      return systemTaskEventUrl;
    }

    public void setSystemTaskEventUrl(String systemTaskEventUrl) {
      this.systemTaskEventUrl = systemTaskEventUrl;
    }

    public String getCamunda7EngineIdentifier() {
      return camunda7EngineIdentifier;
    }

    public void setCamunda7EngineIdentifier(String camundaEngineIdentifier) {
      this.camunda7EngineIdentifier = camundaEngineIdentifier;
    }

    @Override
    public String toString() {
      return "SystemUrlInfo [systemRestUrl="
          + systemRestUrl
          + ", systemTaskEventUrl="
          + systemTaskEventUrl
          + ", camunda7EngineIdentifier="
          + camunda7EngineIdentifier
          + "]";
    }
  }
}
