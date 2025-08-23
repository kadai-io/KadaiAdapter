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

/**
 * Interface for outbound operations - sending commands to external systems.
 */
public interface OutboundSystemConnector extends BaseSystemConnector {

  /**
   * Instruct the external system to complete a task.
   *
   * @param task the task to be completed.
   * @return the response from the external system.
   */
  SystemResponse completeReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to claim a task.
   *
   * @param task the task to be claimed.
   * @return the response from the external system.
   */
  SystemResponse claimReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to cancel claim on a task.
   *
   * @param task the task to cancel the claim on.
   * @return the response from the external system.
   */
  SystemResponse cancelClaimReferencedTask(ReferencedTask task);
}
