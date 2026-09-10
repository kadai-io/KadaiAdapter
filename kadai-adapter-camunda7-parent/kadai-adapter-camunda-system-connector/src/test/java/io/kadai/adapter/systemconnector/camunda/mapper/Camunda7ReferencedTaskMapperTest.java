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

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.Camunda7TaskEvent;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.dto.Camunda7ReferencedTaskDto;
import org.junit.jupiter.api.Test;

class Camunda7ReferencedTaskMapperTest {

  private final Camunda7ReferencedTaskMapper mapper = new Camunda7ReferencedTaskMapper();

  @Test
  void should_MapPayloadFieldsAndEnvelopeMetadata() {
    Camunda7ReferencedTaskDto dto = new Camunda7ReferencedTaskDto();
    dto.setId("id");
    dto.setName("name");
    dto.setAssignee("assignee");
    dto.setCreated("created");
    dto.setPlanned("planned");
    dto.setDue("due");
    dto.setDescription("description");
    dto.setOwner("owner");
    dto.setPriority("priority");
    dto.setManualPriority("manualPriority");
    dto.setTaskDefinitionKey("taskDefinitionKey");
    dto.setBusinessProcessId("businessProcessId");
    dto.setVariables("{\"variable\":\"value\"}");
    dto.setTaskState("COMPLETED");
    dto.setDomain("domain");
    dto.setClassificationKey("classificationKey");
    dto.setWorkbasketKey("workbasketKey");
    dto.setCustomInt1("customInt1");
    dto.setCustomInt2("customInt2");
    dto.setCustomInt3("customInt3");
    dto.setCustomInt4("customInt4");
    dto.setCustomInt5("customInt5");
    dto.setCustomInt6("customInt6");
    dto.setCustomInt7("customInt7");
    dto.setCustomInt8("customInt8");
    Camunda7TaskEvent event = new Camunda7TaskEvent();
    event.setId(12);
    event.setType("create");

    ReferencedTask actual = mapper.toReferencedTask(dto, event);

    assertThat(actual.getId()).isEqualTo("id");
    assertThat(actual.getName()).isEqualTo("name");
    assertThat(actual.getAssignee()).isEqualTo("assignee");
    assertThat(actual.getCreated()).isEqualTo("created");
    assertThat(actual.getPlanned()).isEqualTo("planned");
    assertThat(actual.getDue()).isEqualTo("due");
    assertThat(actual.getDescription()).isEqualTo("description");
    assertThat(actual.getOwner()).isEqualTo("owner");
    assertThat(actual.getPriority()).isEqualTo("priority");
    assertThat(actual.getManualPriority()).isEqualTo("manualPriority");
    assertThat(actual.getTaskDefinitionKey()).isEqualTo("taskDefinitionKey");
    assertThat(actual.getBusinessProcessId()).isEqualTo("businessProcessId");
    assertThat(actual.getVariables()).isEqualTo("{\"variable\":\"value\"}");
    assertThat(actual.getTaskState()).isEqualTo("COMPLETED");
    assertThat(actual.getDomain()).isEqualTo("domain");
    assertThat(actual.getClassificationKey()).isEqualTo("classificationKey");
    assertThat(actual.getWorkbasketKey()).isEqualTo("workbasketKey");
    assertThat(actual.getCustomInt1()).isEqualTo("customInt1");
    assertThat(actual.getCustomInt2()).isEqualTo("customInt2");
    assertThat(actual.getCustomInt3()).isEqualTo("customInt3");
    assertThat(actual.getCustomInt4()).isEqualTo("customInt4");
    assertThat(actual.getCustomInt5()).isEqualTo("customInt5");
    assertThat(actual.getCustomInt6()).isEqualTo("customInt6");
    assertThat(actual.getCustomInt7()).isEqualTo("customInt7");
    assertThat(actual.getCustomInt8()).isEqualTo("customInt8");
    assertThat(actual.getOutboxEventId()).isEqualTo("12");
    assertThat(actual.getOutboxEventType()).isEqualTo("create");
    assertThat(actual.getSystemUrl()).isNull();
  }
}
