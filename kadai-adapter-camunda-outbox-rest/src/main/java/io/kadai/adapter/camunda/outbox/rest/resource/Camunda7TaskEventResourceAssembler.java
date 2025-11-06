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

package io.kadai.adapter.camunda.outbox.rest.resource;

import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;

/**
 * Transforms {@link Camunda7TaskEvent} to its resource counterpart {@link Camunda7TaskEventResource}
 * and vice versa.
 */
public class Camunda7TaskEventResourceAssembler {

  public Camunda7TaskEventResource toResource(Camunda7TaskEvent camunda7TaskEvent) {

    Camunda7TaskEventResource camunda7TaskEventResource = new Camunda7TaskEventResource();

    camunda7TaskEventResource.setId(camunda7TaskEvent.getId());
    camunda7TaskEventResource.setCreated(camunda7TaskEvent.getCreated());
    camunda7TaskEventResource.setType(camunda7TaskEvent.getType());
    camunda7TaskEventResource.setPayload(camunda7TaskEvent.getPayload());
    camunda7TaskEventResource.setRemainingRetries(camunda7TaskEvent.getRemainingRetries());
    camunda7TaskEventResource.setBlockedUntil(camunda7TaskEvent.getBlockedUntil());
    camunda7TaskEventResource.setError(camunda7TaskEvent.getError());
    camunda7TaskEventResource.setCamundaTaskId(camunda7TaskEvent.getCamundaTaskId());
    camunda7TaskEventResource.setLockExpiresAt(camunda7TaskEvent.getLockExpiresAt());

    return camunda7TaskEventResource;
  }

  public Camunda7TaskEvent toModel(Camunda7TaskEventResource camunda7TaskEventResource) {

    Camunda7TaskEvent camunda7TaskEvent = new Camunda7TaskEvent();

    camunda7TaskEvent.setId(camunda7TaskEventResource.getId());
    camunda7TaskEvent.setCreated(camunda7TaskEventResource.getCreated());
    camunda7TaskEvent.setType(camunda7TaskEventResource.getType());
    camunda7TaskEvent.setPayload(camunda7TaskEventResource.getPayload());
    camunda7TaskEvent.setRemainingRetries(camunda7TaskEventResource.getRemainingRetries());
    camunda7TaskEvent.setBlockedUntil(camunda7TaskEventResource.getBlockedUntil());
    camunda7TaskEvent.setError(camunda7TaskEventResource.getError());
    camunda7TaskEvent.setCamundaTaskId(camunda7TaskEventResource.getCamundaTaskId());
    camunda7TaskEvent.setLockExpiresAt(camunda7TaskEventResource.getLockExpiresAt());

    return camunda7TaskEvent;
  }
}
