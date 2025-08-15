/*
 * Copyright [2025] [envite consulting GmbH]
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

package io.kadai.adapter.impl;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.systemconnector.api.ReferencedTask;

/**
 * Service interface for completing KADAI tasks.
 * This service handles the core business logic of task completion without orchestration concerns.
 */
public interface KadaiTaskCompletionService {

  /**
   * Terminates a KADAI task based on a referenced task.
   *
   * @param referencedTask the referenced task for which to terminate the KADAI task
   * @throws TaskTerminationFailedException if the task termination fails
   */
  void terminateKadaiTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException;
}
