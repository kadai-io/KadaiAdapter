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

package io.kadai.adapter.systemconnector.api;

import java.util.List;

/**
 * Interface for inbound operations - retrieving data from external systems and handling
 * notifications about KADAI task lifecycle events.
 */
public interface InboundSystemConnector extends BaseSystemConnector {

  /**
   * Retrieve ReferencedTasks that were started within the last polling interval.
   *
   * @return a list of created ReferencedTasks that don't have an associated KADAI task yet.
   */
  List<ReferencedTask> retrieveNewStartedReferencedTasks();

  /**
   * Retrieve ReferencedTasks that were finished.
   *
   * @return a list of ReferencedTasks that were finished
   */
  List<ReferencedTask> retrieveFinishedReferencedTasks();

  /**
   * Get the variables of the ReferencedTask.
   *
   * @param taskId the Id of the ReferencedTask.
   * @return the variables of the ReferencedTask.
   */
  String retrieveReferencedTaskVariables(String taskId);

  /**
   * With this call the Adapter notifies the SystemConnector that a list of KADAI tasks has been
   * created. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which KADAI tasks have been created.
   */
  void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * With this call the Adapter notifies the SystemConnector that a list of KADAI tasks has been
   * terminated. The rationale for this action is that ReferencedTasks in the external system were
   * finished. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which KADAI Tasks have been terminated.
   */
  void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * With this call the Adapter notifies the SystemConnector that a KADAI task has failed to be
   * created. Depending on the implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTask The ReferencedTasks for which the KADAI task failed to be created
   * @param e exception
   */
  void kadaiTaskFailedToBeCreatedForNewReferencedTask(ReferencedTask referencedTask, Exception e);

  /**
   * Instruct the external system to unlock the event.
   *
   * @param eventId the id of the event that needs to be unlocked
   */
  void unlockEvent(String eventId);
}
