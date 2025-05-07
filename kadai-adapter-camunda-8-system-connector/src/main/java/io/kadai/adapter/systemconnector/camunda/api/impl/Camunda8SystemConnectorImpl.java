package io.kadai.adapter.systemconnector.camunda.api.impl;

import java.util.List;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.api.SystemResponse;

public class Camunda8SystemConnectorImpl implements SystemConnector{

  @Override
  public List<ReferencedTask> retrieveNewStartedReferencedTasks() {
    return List.of();
  }

  @Override
  public void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks) {

  }

  @Override
  public List<ReferencedTask> retrieveFinishedReferencedTasks() {
    return List.of();
  }

  @Override
  public void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks) {

  }

  @Override
  public String retrieveReferencedTaskVariables(String taskId) {
    return "";
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask task) {
    return null;
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask task) {
    return null;
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask task) {
    return null;
  }

  @Override
  public String getSystemUrl() {
    return "";
  }

  @Override
  public String getSystemIdentifier() {
    return "";
  }

  @Override
  public void kadaiTaskFailedToBeCreatedForNewReferencedTask(ReferencedTask referencedTask,
      Exception e) {

  }

  @Override
  public void unlockEvent(String eventId) {

  }
}
