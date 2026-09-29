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
 */

package io.kadai.adapter.systemconnector.camunda.mapper;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.dto.Camunda7ReferencedTaskDto;
import io.kadai.task.api.TaskState;
import org.springframework.stereotype.Component;

/**
 * Maps a deserialized Camunda 7 task-event payload to the adapter core {@code ReferencedTask}.
 */
@Component
public class Camunda7ReferencedTaskMapper {

  public ReferencedTask toReferencedTask(Camunda7ReferencedTaskDto dto) {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId(dto.getId());
    referencedTask.setName(dto.getName());
    referencedTask.setAssignee(dto.getAssignee());
    referencedTask.setCreated(dto.getCreated());
    referencedTask.setPlanned(dto.getPlanned());
    referencedTask.setDue(dto.getDue());
    referencedTask.setDescription(dto.getDescription());
    referencedTask.setManualPriority(dto.getManualPriority());
    referencedTask.setTaskDefinitionKey(dto.getTaskDefinitionKey());
    referencedTask.setBusinessProcessId(dto.getBusinessProcessId());
    referencedTask.setVariables(dto.getVariables());
    referencedTask.setTaskState(mapTaskState(dto.getTaskState()));
    referencedTask.setDomain(dto.getDomain());
    referencedTask.setClassificationKey(dto.getClassificationKey());
    referencedTask.setWorkbasketKey(dto.getWorkbasketKey());
    referencedTask.setCustomInt1(dto.getCustomInt1());
    referencedTask.setCustomInt2(dto.getCustomInt2());
    referencedTask.setCustomInt3(dto.getCustomInt3());
    referencedTask.setCustomInt4(dto.getCustomInt4());
    referencedTask.setCustomInt5(dto.getCustomInt5());
    referencedTask.setCustomInt6(dto.getCustomInt6());
    referencedTask.setCustomInt7(dto.getCustomInt7());
    referencedTask.setCustomInt8(dto.getCustomInt8());
    return referencedTask;
  }

  private TaskState mapTaskState(String taskState) {
    if (taskState == null) {
      return null;
    }
    return TaskState.valueOf(taskState);
  }
}
