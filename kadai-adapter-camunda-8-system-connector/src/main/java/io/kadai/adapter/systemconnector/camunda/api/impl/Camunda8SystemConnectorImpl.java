package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;

/** Sample Implementation of Camunda8SystemConnector. */
public class Camunda8SystemConnectorImpl implements OutboundSystemConnector {

  static final String URL_GET_CAMUNDA8_USER_TASKS = "/v1/user-tasks/";
  static final String URL_CAMUNDA8_ASSIGNMENT = "/assignment";
  static final String URL_CAMUNDA8_COMPLETION = "/completion";
  static final String URL_CAMUNDA8_UNCLAIM = "/assignee";

  static final String BODY_CAMUNDA8_ASSIGN = "{\"assignee\": \"%s\", "
          + "\"allowOverride\": true, "
          + "\"action\": \"assign\"}";
  static final String BODY_CAMUNDA8_COMPLETE = "{\"variables\": {}, \"action\": \"complete\"}";
  static final String BODY_EMPTY_REQUEST = "{}";

  private final Camunda8System camunda8System;

  private final Camunda8TaskClaimer taskClaimer;

  private final Camunda8TaskCompleter taskCompleter;

  private final Camunda8TaskClaimCanceler  taskClaimCanceler;

  public Camunda8SystemConnectorImpl(Camunda8System camunda8System) {
    this.camunda8System = camunda8System;
    taskClaimer = AdapterSpringContextProvider.getBean(Camunda8TaskClaimer.class);
    taskCompleter = AdapterSpringContextProvider.getBean(Camunda8TaskCompleter.class);
    taskClaimCanceler = AdapterSpringContextProvider.getBean(Camunda8TaskClaimCanceler.class);
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
    return camunda8System.getSystemUrl();
  }

  @Override
  public String toString() {
    return "Camunda8SystemConnectorImpl [camunda8System=" + camunda8System + "]";
  }
}
