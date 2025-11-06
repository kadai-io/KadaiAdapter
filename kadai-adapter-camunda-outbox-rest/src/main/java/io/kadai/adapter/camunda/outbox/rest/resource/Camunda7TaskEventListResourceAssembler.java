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

import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEventList;

/**
 * Transforms {@link Camunda7TaskEventList} to its resource counterpart {@link
 * Camunda7TaskEventListResource} and vice versa.
 */
public class Camunda7TaskEventListResourceAssembler {

  public Camunda7TaskEventListResource toResource(Camunda7TaskEventList camunda7TaskEventList) {
    return new Camunda7TaskEventListResource(camunda7TaskEventList);
  }

  public Camunda7TaskEventList toModel(
      Camunda7TaskEventListResource camunda7TaskEventListResource) {
    Camunda7TaskEventList camunda7TaskEventList = new Camunda7TaskEventList();
    camunda7TaskEventList.setCamundaTaskEvents(
        camunda7TaskEventListResource.getCamundaTaskEvents());

    return camunda7TaskEventList;
  }
}
