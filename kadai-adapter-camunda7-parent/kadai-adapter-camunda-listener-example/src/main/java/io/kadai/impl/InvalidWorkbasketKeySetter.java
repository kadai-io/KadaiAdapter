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

package io.kadai.impl;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

/**
 * Sets an intentionally invalid workbasket key on a Camunda process execution. Used in integration
 * tests to exercise error-handling paths when the kadai adapter cannot find the target workbasket.
 */
public class InvalidWorkbasketKeySetter implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    execution.setVariable("kadai.workbasket-key", "invalidWorkbasketKey");
  }
}
