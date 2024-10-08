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

package io.kadai.adapter.exceptions;

import io.kadai.common.api.exceptions.ErrorCode;
import io.kadai.common.api.exceptions.KadaiException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** This exception is thrown when the adapter failed to terminate a task in kadai. */
public class TaskTerminationFailedException extends KadaiException {

  public static final String ERROR_KEY = "TASK_TERMINATION_FAILED";
  private final String taskId;

  public TaskTerminationFailedException(String taskId, Throwable cause) {
    super(
        String.format("Task termination failed for task with id '%s'", taskId),
        ErrorCode.of(ERROR_KEY, createUnmodifiableMap("taskId", taskId)),
        cause);
    this.taskId = taskId;
  }

  public String getTaskId() {
    return taskId;
  }

  private static Map<String, Serializable> createUnmodifiableMap(String key, Serializable value) {
    HashMap<String, Serializable> map = new HashMap<>();
    map.put(key, ensureNullIsHandled(value));
    return Collections.unmodifiableMap(map);
  }
}
