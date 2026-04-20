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

package io.kadai.adapter.config;

import io.kadai.adapter.camunda.outbox.rest.controller.Camunda7TaskEventsController;
import io.kadai.adapter.camunda.outbox.rest.exception.Camunda7TaskEventNotFoundExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.JsonParseExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.MismatchedInputExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Jersey JAX-RS configuration for the embedded Outbox REST service.
 *
 * <p>Registers all JAX-RS resources and exception mappers from the {@code
 * kadai-adapter-camunda-outbox-rest} module. The application path {@code /outbox-rest} is set via
 * {@code spring.jersey.application-path} in {@code application.properties}.
 */
@Component
public class OutboxJerseyConfig extends ResourceConfig {

  public OutboxJerseyConfig() {
    register(Camunda7TaskEventsController.class);
    register(InvalidArgumentExceptionMapper.class);
    register(Camunda7TaskEventNotFoundExceptionMapper.class);
    register(JsonParseExceptionMapper.class);
    register(MismatchedInputExceptionMapper.class);
  }
}
