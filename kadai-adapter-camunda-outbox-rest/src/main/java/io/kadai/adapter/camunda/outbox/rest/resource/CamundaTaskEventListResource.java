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
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEventList;
import java.io.Serializable;
import java.util.List;

/**
 * This class wraps a list of CamundaTaskEventResources in a seperate object. This is needed to make
 * the REST-API work with Jersey
 */
public class CamundaTaskEventListResource implements Serializable {

  private List<CamundaTaskEvent> camundaTaskEvents;

  public CamundaTaskEventListResource() {}

  public CamundaTaskEventListResource(
      CamundaTaskEventList camundaTaskEventList) {
    this.camundaTaskEvents = camundaTaskEventList.getCamundaTaskEvents();
  }

  public List<CamundaTaskEvent> getCamundaTaskEvents() {
    return camundaTaskEvents;
  }

  public void setCamundaTaskEvents(List<CamundaTaskEvent> theResources) {
    this.camundaTaskEvents = theResources;
  }
}
