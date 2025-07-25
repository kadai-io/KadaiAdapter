package io.kadai.adapter.systemconnector.camunda.spi.impl;

import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8SystemConnectorImpl;
import io.kadai.adapter.systemconnector.spi.SystemConnectorProvider;
import java.util.List;

public class SystemConnectorCamunda8ProviderImpl implements SystemConnectorProvider {

  @Override
  public List<SystemConnector> create() {
    // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we
    // must
    // retrieve the Spring-generated Bean for camundaSystemUrls programatically. Only this bean has
    // the properties
    // resolved.
    // In order for this bean to be retrievable, the SpringContextProvider must already be
    // initialized.
    // This is assured via the
    // @DependsOn(value= {"adapterSpringContextProvider"}) annotation of
    // CamundaSystemConnectorConfiguration

    // System Urls duplication?
    return List.of(new Camunda8SystemConnectorImpl());
  }
}
