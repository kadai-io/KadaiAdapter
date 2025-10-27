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

import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.models.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KadaiTaskStarterServiceImpl implements KadaiTaskStarterService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiTaskStarterServiceImpl.class);

  private final AdapterManager adapterManager;

  public KadaiTaskStarterServiceImpl(AdapterManager adapterManager) {
    this.adapterManager = adapterManager;
  }

  @Override
  public void createKadaiTask(ReferencedTask referencedTask) throws TaskCreationFailedException {
    LOGGER.trace("KadaiTaskStarterService.createKadaiTask ENTRY");

    KadaiConnector kadaiConnector = adapterManager.getKadaiConnector();

    Task kadaiTask = kadaiConnector.convertToKadaiTask(referencedTask);
    kadaiConnector.createKadaiTask(kadaiTask);

    LOGGER.trace("KadaiTaskStarterService.createKadaiTask EXIT");
  }
}
