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

package io.kadai.adapter.manager;

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.kadaiconnector.spi.KadaiConnectorProvider;
import io.kadai.adapter.systemconnector.api.InboundSystemConnector;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.spi.InboundSystemConnectorProvider;
import io.kadai.adapter.systemconnector.spi.OutboundSystemConnectorProvider;
import io.kadai.adapter.util.Assert;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Scheduler for receiving referenced tasks, completing Kadai tasks and cleaning adapter tables. */
@Component
public class AdapterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdapterManager.class);
  private boolean isInitialized = false;

  private Map<String, InboundSystemConnector> inboundSystemConnectors;
  private Map<String, OutboundSystemConnector> outboundSystemConnectors;
  private List<KadaiConnector> kadaiConnectors;

  public Map<String, InboundSystemConnector> getInboundSystemConnectors() {
    return inboundSystemConnectors;
  }

  public Map<String, OutboundSystemConnector> getOutboundSystemConnectors() {
    return outboundSystemConnectors;
  }

  public KadaiConnector getKadaiConnector() {
    Assert.assertion(kadaiConnectors.size() == 1, "kadaiConnectors.size() == 1");
    return kadaiConnectors.get(0);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void init() {
    if (isInitialized) {
      return;
    }
    LOGGER.debug("initAdapterInfrastructure called ");
    initKadaiConnectors();
    initSystemConnectors();
    isInitialized = true;
  }

  private void initSystemConnectors() {
    inboundSystemConnectors = new HashMap<>();
    outboundSystemConnectors = new HashMap<>();
    LOGGER.info("initializing system connectors ");

    ServiceLoader<InboundSystemConnectorProvider> inboundLoader =
        ServiceLoader.load(InboundSystemConnectorProvider.class);
    for (InboundSystemConnectorProvider provider : inboundLoader) {
      List<InboundSystemConnector> connectors = provider.create();
      for (InboundSystemConnector conn : connectors) {
        inboundSystemConnectors.put(conn.getSystemUrl(), conn);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              "initialized inbound system connector {} for system_url {}",
              conn,
              conn.getSystemUrl());
        }
      }
    }

    ServiceLoader<OutboundSystemConnectorProvider> outboundLoader =
        ServiceLoader.load(OutboundSystemConnectorProvider.class);
    for (OutboundSystemConnectorProvider provider : outboundLoader) {
      List<OutboundSystemConnector> connectors = provider.create();
      for (OutboundSystemConnector conn : connectors) {
        outboundSystemConnectors.put(conn.getSystemUrl(), conn);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              "initialized outbound system connector {} for system_url {}",
              conn,
              conn.getSystemUrl());
        }
      }
    }
  }

  private void initKadaiConnectors() {
    kadaiConnectors = new ArrayList<>();
    ServiceLoader<KadaiConnectorProvider> loader = ServiceLoader.load(KadaiConnectorProvider.class);
    for (KadaiConnectorProvider provider : loader) {
      List<KadaiConnector> connectors = provider.create();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("initialized kadai connectors {} ", connectors);
      }
      kadaiConnectors.addAll(connectors);
    }
  }
}
