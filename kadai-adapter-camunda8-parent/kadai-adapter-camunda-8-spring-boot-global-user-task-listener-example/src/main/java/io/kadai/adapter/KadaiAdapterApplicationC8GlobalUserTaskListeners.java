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

package io.kadai.adapter;

import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.CANCELING;
import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.COMPLETING;
import static io.camunda.client.api.search.enums.GlobalTaskListenerEventType.CREATING;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.GlobalTaskListenerEventType;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCancellation;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCompletion;
import io.kadai.adapter.systemconnector.camunda.tasklistener.UserTaskCreation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Application that provides an adapter between KADAI and one or more external systems. */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class KadaiAdapterApplicationC8GlobalUserTaskListeners {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KadaiAdapterApplicationC8GlobalUserTaskListeners.class);

  public static void main(String[] args) {
    SpringApplication.run(KadaiAdapterApplicationC8GlobalUserTaskListeners.class, args);
  }

  @Bean
  ApplicationRunner configureGlobalUserTaskListeners(CamundaClient client) {
    return args -> {
      List<GlobalUserTaskListenerDefinition> globalListeners =
          List.of(
              new GlobalUserTaskListenerDefinition(
                  "kadai-create-task",
                  UserTaskCreation.USER_TASK_CREATED_JOB_WORKER_TYPE,
                  CREATING),
              new GlobalUserTaskListenerDefinition(
                  "kadai-complete-task",
                  UserTaskCompletion.USER_TASK_COMPLETED_JOB_WORKER_TYPE,
                  COMPLETING),
              // Camunda 8.9 currently has a known defect:
              // https://github.com/camunda/camunda/issues/51630
              // Global canceling listeners are not triggered unless a model-level canceling
              // listener also exists (which unfortunately attempts to cancel twice).
              new GlobalUserTaskListenerDefinition(
                  "kadai-cancel-task",
                  UserTaskCancellation.USER_TASK_CANCELLED_JOB_WORKER_TYPE,
                  CANCELING));

      globalListeners.forEach(listener -> configureGlobalUserTaskListener(client, listener));
    };
  }

  private void configureGlobalUserTaskListener(
      CamundaClient client, GlobalUserTaskListenerDefinition listener) {
    deleteGlobalUserTaskListenerIfPresent(client, listener.id());

    client
        .newCreateGlobalTaskListenerRequest()
        .id(listener.id())
        .type(listener.type())
        .eventTypes(listener.eventType())
        .priority(50)
        .send()
        .join();

    LOGGER.info(
        "Configured Camunda global user task listener '{}' for event '{}' with type '{}'",
        listener.id(),
        listener.eventType(),
        listener.type());
  }

  private void deleteGlobalUserTaskListenerIfPresent(CamundaClient client, String id) {
    try {
      client.newDeleteGlobalTaskListenerRequest(id).send().join();
    } catch (RuntimeException e) {
      LOGGER.debug("Global user task listener '{}' did not exist before startup", id, e);
    }
  }

  private record GlobalUserTaskListenerDefinition(
      String id, String type, GlobalTaskListenerEventType eventType) {}
}
