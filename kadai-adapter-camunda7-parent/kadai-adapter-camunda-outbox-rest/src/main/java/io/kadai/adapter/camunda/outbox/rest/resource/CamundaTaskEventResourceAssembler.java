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

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/**
 * Transforms {@link CamundaTaskEvent} to its resource counterpart {@link CamundaTaskEventResource}
 * and vice versa.
 */
public class CamundaTaskEventResourceAssembler {

  public CamundaTaskEventResource toResource(CamundaTaskEvent camundaTaskEvent) {

    CamundaTaskEventResource camundaTaskEventResource = new CamundaTaskEventResource();

    camundaTaskEventResource.setId(camundaTaskEvent.getId());
    camundaTaskEventResource.setCreated(camundaTaskEvent.getCreated());
    camundaTaskEventResource.setType(camundaTaskEvent.getType());
    camundaTaskEventResource.setPayload(camundaTaskEvent.getPayload());
    camundaTaskEventResource.setRemainingRetries(camundaTaskEvent.getRemainingRetries());
    camundaTaskEventResource.setBlockedUntil(camundaTaskEvent.getBlockedUntil());
    camundaTaskEventResource.setError(camundaTaskEvent.getError());
    camundaTaskEventResource.setCamundaTaskId(camundaTaskEvent.getCamundaTaskId());
    camundaTaskEventResource.setLockExpiresAt(camundaTaskEvent.getLockExpiresAt());

    return camundaTaskEventResource;
  }

  public CamundaTaskEvent toModel(CamundaTaskEventResource camundaTaskEventResource) {

    CamundaTaskEvent camundaTaskEvent = new CamundaTaskEvent();

    camundaTaskEvent.setId(camundaTaskEventResource.getId());
    camundaTaskEvent.setCreated(camundaTaskEventResource.getCreated());
    camundaTaskEvent.setType(camundaTaskEventResource.getType());
    camundaTaskEvent.setPayload(camundaTaskEventResource.getPayload());
    camundaTaskEvent.setRemainingRetries(camundaTaskEventResource.getRemainingRetries());
    camundaTaskEvent.setBlockedUntil(camundaTaskEventResource.getBlockedUntil());
    camundaTaskEvent.setError(camundaTaskEventResource.getError());
    camundaTaskEvent.setCamundaTaskId(camundaTaskEventResource.getCamundaTaskId());
    camundaTaskEvent.setLockExpiresAt(camundaTaskEventResource.getLockExpiresAt());

    return camundaTaskEvent;
  }
}
