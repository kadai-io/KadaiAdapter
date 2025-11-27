package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;

/** Implementation of OutboundSystemConnector for Camunda 8. */
public class Camunda8SystemConnectorImpl implements OutboundSystemConnector {

  static final String URL_GET_CAMUNDA8_USER_TASKS = "/v2/user-tasks/";
  static final String URL_CAMUNDA8_ASSIGNMENT = "/assignment";
  static final String URL_CAMUNDA8_COMPLETION = "/completion";
  static final String URL_CAMUNDA8_UNCLAIM = "/assignee";

  static final String BODY_CAMUNDA8_ASSIGN =
      "{\"assignee\": \"%s\", " + "\"allowOverride\": true, " + "\"action\": \"assign\"}";
  static final String BODY_CAMUNDA8_COMPLETE = "{\"variables\": {}, \"action\": \"complete\"}";
  static final String BODY_EMPTY_REQUEST = "{}";

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
    return taskCompleter.completeCamunda8Task(camunda8System, camundaTask);
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimer.claimCamunda8Task(camunda8System, camundaTask);
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimCanceler.cancelClaimOfCamunda8Task(camunda8System, camundaTask);
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
