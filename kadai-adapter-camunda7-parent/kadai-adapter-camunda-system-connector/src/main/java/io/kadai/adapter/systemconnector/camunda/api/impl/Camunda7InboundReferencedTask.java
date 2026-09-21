/*
 * Copyright [2026] [envite consulting GmbH]
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
 */

package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.InboundReferencedTask;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import jakarta.annotation.Nonnull;
import java.util.Objects;

/** A referenced task together with the Camunda 7 event that delivered it. */
public record Camunda7InboundReferencedTask(ReferencedTask referencedTask, int taskEventId)
    implements InboundReferencedTask {

  public Camunda7InboundReferencedTask(ReferencedTask referencedTask, int taskEventId) {
    this.referencedTask = Objects.requireNonNull(referencedTask, "referencedTask must not be null");
    this.taskEventId = taskEventId;
  }

  @Nonnull
  @Override
  public String toString() {
    return "Camunda7InboundReferencedTask [referencedTask="
        + referencedTask
        + ", taskEventId="
        + taskEventId
        + "]";
  }
}
