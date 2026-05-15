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

package io.kadai.adapter.test.configuration;

import io.kadai.adapter.camunda.outbox.rest.controller.Camunda7TaskEventsController;
import io.kadai.adapter.camunda.outbox.rest.exception.Camunda7TaskEventNotFoundExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.JsonParseExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.MismatchedInputExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Jersey JAX-RS configuration for the outbox REST service running in the test JVM.
 *
 * <p>The Camunda engine runs in a Docker container (via Testcontainers) and writes task events to a
 * shared PostgreSQL outbox table. The kadai-adapter Camunda 7 system connector reads those events
 * by calling the outbox REST API. Rather than running RESTEasy inside the Camunda container, we
 * serve the same JAX-RS resources here in the Spring Boot 4 test JVM via Jersey
 * (spring-boot-starter-jersey), which natively supports Jakarta EE 11 / JAX-RS 3.
 *
 * <p>The {@link Camunda7TaskEventsController} accesses the outbox table via the JDBC URL provided
 * by {@code System.setProperty("kadai.outbox.properties", ...)} during container initialization.
 */
@Component
@ApplicationPath("/outbox-rest")
public class OutboxRestJerseyConfig extends ResourceConfig {

  public OutboxRestJerseyConfig() {
    register(Camunda7TaskEventsController.class);
    register(InvalidArgumentExceptionMapper.class);
    register(Camunda7TaskEventNotFoundExceptionMapper.class);
    register(JsonParseExceptionMapper.class);
    register(MismatchedInputExceptionMapper.class);
  }
}
