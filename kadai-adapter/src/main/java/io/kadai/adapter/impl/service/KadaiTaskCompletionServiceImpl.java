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

package io.kadai.adapter.impl.service;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.impl.util.UserContext;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.common.internal.util.CheckedSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KadaiTaskCompletionServiceImpl implements KadaiTaskCompletionService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KadaiTaskCompletionServiceImpl.class);

  private final AdapterManager adapterManager;

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  public KadaiTaskCompletionServiceImpl(AdapterManager adapterManager) {
    this.adapterManager = adapterManager;
  }

  @Override
  public void terminateKadaiTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException {
    LOGGER.trace("KadaiTaskCompletionService.terminateKadaiTask ENTRY");

    UserContext.runAsUser(
        runAsUser,
        CheckedSupplier.rethrowing(
            () -> {
              adapterManager.getKadaiConnector().terminateKadaiTask(referencedTask);
              return null;
            }
        )
    );

    LOGGER.trace("KadaiTaskCompletionService.terminateKadaiTask EXIT");
  }
}
