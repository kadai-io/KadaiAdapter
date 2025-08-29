package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;

public class Camunda8SystemConnectorImpl implements OutboundSystemConnector {

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask task) {
    // todo: implement: call Camunda 8 API to complete the task
    return null;
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask task) {
    //todo: implement: call Camunda 8 API to claim the task
    return null;
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask task) {
    //todo: implement: call Camunda 8 API to cancel the claim of the task
    return null;
  }

  @Override
  public String getSystemUrl() {
    // todo: implement: return the system URL of the Camunda 8 instance
    return "";
  }

  @Override
  public String getSystemIdentifier() {
    // todo: implement: return the system identifier of the Camunda 8 instance
    return "";
  }
}
