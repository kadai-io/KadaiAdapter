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

package io.kadai.adapter.systemconnector.camunda.spi.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda7SystemConnectorImpl;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7SystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.spi.OutboundSystemConnectorProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements OutboundSystemConnectorProvider for Camunda. Creates Camunda7SystemConnectorImpl
 * instances for outbound operations.
 */
public class OutboundSystemConnectorCamunda7ProviderImpl
    implements OutboundSystemConnectorProvider {

  @Override
  public List<OutboundSystemConnector> create() {
    // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we
    // must
    // retrieve the Spring-generated Bean for camundaSystemUrls programatically. Only this bean has
    // the properties
    // resolved.
    // In order for this bean to be retrievable, the SpringContextProvider must already be
    // initialized.
    // This is assured via the
    // @DependsOn(value= {"adapterSpringContextProvider"}) annotation of
    // Camunda7SystemConnectorConfiguration

    Camunda7SystemConnectorConfiguration camunda7SystemConfigurationProperties =
        AdapterSpringContextProvider.getBean(Camunda7SystemConnectorConfiguration.class);

    List<OutboundSystemConnector> result = new ArrayList<>();
    for (Camunda7System camunda7System : camunda7SystemConfigurationProperties.getSystems()) {
      Camunda7SystemConnectorImpl camundaSystemConnector =
          new Camunda7SystemConnectorImpl(camunda7System);
      result.add(camundaSystemConnector);
    }
    return result;
  }
}
