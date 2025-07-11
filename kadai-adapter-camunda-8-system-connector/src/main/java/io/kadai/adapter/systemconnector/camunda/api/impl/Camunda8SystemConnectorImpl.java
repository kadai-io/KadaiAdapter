package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import java.util.List;

public class Camunda8SystemConnectorImpl implements SystemConnector {

  @Override
  public List<ReferencedTask> retrieveNewStartedReferencedTasks() {

    // This method is not used because we use the UserTaskListener to retrieve the newly started
    // tasks from Camunda 8. TODO: provide method reference

    return List.of();
  }

  @Override
  public void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks) {
    // todo: implement: call Camunda 8 API to create the tasks in Camunda 8
  }

  @Override
  public List<ReferencedTask> retrieveFinishedReferencedTasks() {

    // This method is not used because we use the UserTaskListener to retrieve the finished
    // tasks from Camunda 8. TODO: provide method reference

    return List.of();
  }

  @Override
  public void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks) {
    //todo: implement: call Camunda 8 API to terminate the tasks in Camunda 8
  }

  @Override
  public String retrieveReferencedTaskVariables(String taskId) {
    // todo: method to retrieve the variables of a ReferencedTask from Camunda 8 if the variable of
    //  the given ReferencedTask are null
    return "";
  }

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

  @Override
  public void kadaiTaskFailedToBeCreatedForNewReferencedTask(ReferencedTask referencedTask,
      Exception e) {
      // todo: implement: handle the failure of task creation in Camunda 8
  }

  @Override
  public void unlockEvent(String eventId) {
    // todo: implement but don't know how yet
  }
}
