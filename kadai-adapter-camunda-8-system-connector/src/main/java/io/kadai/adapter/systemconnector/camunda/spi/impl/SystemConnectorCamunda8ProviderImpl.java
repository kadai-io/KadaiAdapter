package io.kadai.adapter.systemconnector.camunda.spi.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8SystemConnectorImpl;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import io.kadai.adapter.systemconnector.spi.OutboundSystemConnectorProvider;
import java.util.List;

public class SystemConnectorCamunda8ProviderImpl implements OutboundSystemConnectorProvider {

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
    // Camunda8SystemConnectorConfiguration

    final Camunda8System camunda8System =
        AdapterSpringContextProvider.getBean(Camunda8System.class);

    // System Urls duplication?
    return List.of(new Camunda8SystemConnectorImpl(camunda8System));
  }
}
