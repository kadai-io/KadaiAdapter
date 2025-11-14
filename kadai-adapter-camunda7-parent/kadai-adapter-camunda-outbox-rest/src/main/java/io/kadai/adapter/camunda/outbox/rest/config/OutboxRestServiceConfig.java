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

package io.kadai.adapter.camunda.outbox.rest.config;

import io.kadai.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import io.kadai.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.JsonParseExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.MismatchedInputExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/** Configures the outbox REST service. */
@ApplicationPath("/outbox-rest")
public class OutboxRestServiceConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classesToBeScanned = new HashSet<>();
    classesToBeScanned.add(CamundaTaskEventsController.class);
    classesToBeScanned.add(InvalidArgumentExceptionMapper.class);
    classesToBeScanned.add(CamundaTaskEventNotFoundExceptionMapper.class);
    classesToBeScanned.add(JsonParseExceptionMapper.class);
    classesToBeScanned.add(MismatchedInputExceptionMapper.class);
    return classesToBeScanned;
  }
}
