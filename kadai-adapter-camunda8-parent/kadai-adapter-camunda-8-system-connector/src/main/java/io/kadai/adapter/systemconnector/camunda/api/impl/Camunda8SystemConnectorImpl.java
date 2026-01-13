package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;

/** Implementation of OutboundSystemConnector for Camunda 8. */
public class Camunda8SystemConnectorImpl implements OutboundSystemConnector {
  private final Camunda8System camunda8System;
  private final Camunda8TaskClaimer taskClaimer;
  private final Camunda8TaskCompleter taskCompleter;
  private final Camunda8TaskClaimCanceler taskClaimCanceler;

  public Camunda8SystemConnectorImpl(
      Camunda8System camunda8System,
      Camunda8TaskClaimer taskClaimer,
      Camunda8TaskCompleter taskCompleter,
      Camunda8TaskClaimCanceler taskClaimCanceler) {
    this.camunda8System = camunda8System;
    this.taskClaimer = taskClaimer;
    this.taskCompleter = taskCompleter;
    this.taskClaimCanceler = taskClaimCanceler;
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
    return taskCompleter.completeCamunda8Task(camundaTask);
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimer.claimCamunda8Task(camundaTask);
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimCanceler.cancelClaimOfCamunda8Task(camundaTask);
  }

  @Override
  public String getSystemUrl() {
    return camunda8System.getRestAddress();
  }

  @Override
  public String toString() {
    return "Camunda8SystemConnectorImpl [camunda8System="
        + camunda8System
        + ", taskClaimer="
        + taskClaimer
        + ", taskCompleter="
        + taskCompleter
        + ", taskClaimCanceler="
        + taskClaimCanceler
        + "]";
  }
}
