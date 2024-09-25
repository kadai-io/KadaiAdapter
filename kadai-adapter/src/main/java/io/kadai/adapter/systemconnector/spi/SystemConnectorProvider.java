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

package io.kadai.adapter.systemconnector.spi;

import io.kadai.adapter.systemconnector.api.SystemConnector;
import java.util.List;

/** The interface for the SystemConnector provider. */
public interface SystemConnectorProvider {

  /**
   * create a list of SystemConnector objects to access multiple external systems.
   *
   * @return a list of SystemConnector objects
   */
  List<SystemConnector> create();
}
