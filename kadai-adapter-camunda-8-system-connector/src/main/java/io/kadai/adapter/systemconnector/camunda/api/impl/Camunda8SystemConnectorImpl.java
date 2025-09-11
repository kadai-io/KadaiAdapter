package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;

public class Camunda8SystemConnectorImpl implements OutboundSystemConnector {

  private final Camunda8System camunda8System;

  public Camunda8SystemConnectorImpl(Camunda8System camunda8System) {
    this.camunda8System = camunda8System;
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask task) {
    // todo (https://github.com/kadai-io/KadaiAdapter/issues/175): implement: call Camunda 8 API to
    // complete the task
    return null;
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask task) {
    // todo (https://github.com/kadai-io/KadaiAdapter/issues/175): implement: call Camunda 8 API to
    // claim the task
    return null;
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask task) {
    // todo (https://github.com/kadai-io/KadaiAdapter/issues/175): implement: call Camunda 8 API to
    // cancel the claim of the task
    return null;
  }

  @Override
  public String getSystemUrl() {
    return camunda8System.getSystemUrl();
  }
}
