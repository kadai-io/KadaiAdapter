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

package io.kadai.adapter.camunda.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests the Camunda 7 task-created event payload model. */
class TaskCreatedEventPayloadTest {

  private final String theValue = "blablabla";
  private TaskCreatedEventPayload theTask;

  TaskCreatedEventPayloadTest() {
    theTask = new TaskCreatedEventPayload();
    theTask.setAssignee("1");
    theTask.setBusinessProcessId("2");
    theTask.setClassificationKey("3");
    theTask.setDescription("bla");
    theTask.setCreated("4");
    theTask.setDomain("5");
    theTask.setPlanned("6");
    theTask.setDue("7");
    theTask.setId("8");
    theTask.setName("9");
    theTask.setOwner("10");
    theTask.setCustomInt1("11");
    theTask.setCustomInt2("12");
    theTask.setCustomInt3("13");
    theTask.setCustomInt4("14");
    theTask.setCustomInt5("15");
    theTask.setCustomInt6("16");
    theTask.setCustomInt7("17");
    theTask.setCustomInt8("18");
  }

  @Test
  void should_ReturnBusinessProcessId_When_BusinessProcessIdWasSet() {
    theTask.setBusinessProcessId(theValue);
    assertThat(theValue).isEqualTo(theTask.getBusinessProcessId());
  }

  @Test
  void should_ReturnId_When_IdWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setId(theValue);
    assertThat(theValue).isEqualTo(payload.getId());
  }

  @Test
  void should_ReturnName_When_NameWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setName(theValue);
    assertThat(theValue).isEqualTo(payload.getName());
  }

  @Test
  void should_ReturnAssignee_When_AssigneeWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setAssignee(theValue);
    assertThat(theValue).isEqualTo(payload.getAssignee());
  }

  @Test
  void should_ReturnCreated_When_CreatedWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setCreated(theValue);
    assertThat(theValue).isEqualTo(payload.getCreated());
  }

  @Test
  void should_ReturnDue_When_DueWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setDue(theValue);
    assertThat(theValue).isEqualTo(payload.getDue());
  }

  @Test
  void should_ReturnDescription_When_DescriptionWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setDescription(theValue);
    assertThat(theValue).isEqualTo(payload.getDescription());
  }

  @Test
  void should_ReturnOwner_When_OwnerWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setOwner(theValue);
    assertThat(theValue).isEqualTo(payload.getOwner());
  }

  @Test
  void should_ReturnPriority_When_PriorityWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setPriority(theValue);
    assertThat(theValue).isEqualTo(payload.getPriority());
  }

  @Test
  void should_ReturnTaskDefinitionKey_When_TaskDefinitionKeyWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setTaskDefinitionKey(theValue);
    assertThat(theValue).isEqualTo(payload.getTaskDefinitionKey());
  }

  @Test
  void should_ReturnVariables_When_VariablesWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setVariables(theValue);
    assertThat(theValue).isEqualTo(payload.getVariables());
  }

  @Test
  void should_ReturnDomain_When_DomainWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setDomain(theValue);
    assertThat(theValue).isEqualTo(payload.getDomain());
  }

  @Test
  void should_ReturnClassificationKey_When_ClassificationKeyWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setClassificationKey(theValue);
    assertThat(theValue).isEqualTo(payload.getClassificationKey());
  }

  @Test
  void should_ReturnWorkbasketKey_When_WorkbasketKeyWasSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setWorkbasketKey(theValue);
    assertThat(theValue).isEqualTo(payload.getWorkbasketKey());
  }

  @Test
  void should_returnCustomIntegers_when_CustomIntegersWereSet() {
    TaskCreatedEventPayload payload = new TaskCreatedEventPayload();
    payload.setCustomInt1("1");
    payload.setCustomInt2("2");
    payload.setCustomInt3("3");
    payload.setCustomInt4("4");
    payload.setCustomInt5("5");
    payload.setCustomInt6("6");
    payload.setCustomInt7("7");
    payload.setCustomInt8("8");

    assertThat(payload.getCustomInt1()).isEqualTo("1");
    assertThat(payload.getCustomInt2()).isEqualTo("2");
    assertThat(payload.getCustomInt3()).isEqualTo("3");
    assertThat(payload.getCustomInt4()).isEqualTo("4");
    assertThat(payload.getCustomInt5()).isEqualTo("5");
    assertThat(payload.getCustomInt6()).isEqualTo("6");
    assertThat(payload.getCustomInt7()).isEqualTo("7");
    assertThat(payload.getCustomInt8()).isEqualTo("8");
  }

  @Test
  void should_ReturnString_When_ToStringIsCalled() {
    assertThat(theTask.toString()).isNotNull();
  }

  @Test
  void should_CheckEquality_When_EqualsIsCalled() {
    TaskCreatedEventPayload secondPayload = theTask;
    secondPayload.setWorkbasketKey("nochnkey");
    assertThat(secondPayload).isEqualTo(theTask);
    secondPayload = theTask;
    assertThat(theTask).isEqualTo(secondPayload).isNotEqualTo("aString").isNotEqualTo(null);
    theTask = new TaskCreatedEventPayload();
    theTask.setAssignee("1");
    theTask.setBusinessProcessId("2");
    theTask.setClassificationKey("3");
    theTask.setDescription("bla");
    theTask.setCreated("4");
    theTask.setDomain("5");
    theTask.setPlanned("6");
    theTask.setDue("7");
    theTask.setId("8");
    theTask.setName("9");
    theTask.setOwner("12");
    theTask.setWorkbasketKey("13");
    theTask.setCustomInt1("14");
    theTask.setCustomInt2("15");
    theTask.setCustomInt3("16");
    theTask.setCustomInt4("17");
    theTask.setCustomInt5("18");
    theTask.setCustomInt6("19");
    theTask.setCustomInt7("20");
    theTask.setCustomInt8("21");

    secondPayload = new TaskCreatedEventPayload();
    secondPayload.setAssignee("1");
    secondPayload.setBusinessProcessId("2");
    secondPayload.setClassificationKey("3");
    secondPayload.setDescription("bla");
    secondPayload.setCreated("4");
    secondPayload.setDomain("5");
    secondPayload.setPlanned("6");
    secondPayload.setDue("7");
    secondPayload.setId("8");
    secondPayload.setName("9");
    secondPayload.setOwner("12");
    secondPayload.setWorkbasketKey("13");
    secondPayload.setCustomInt1("14");
    secondPayload.setCustomInt2("15");
    secondPayload.setCustomInt3("16");
    secondPayload.setCustomInt4("17");
    secondPayload.setCustomInt5("18");
    secondPayload.setCustomInt6("19");
    secondPayload.setCustomInt7("20");
    secondPayload.setCustomInt8("21");

    assertThat(secondPayload).isEqualTo(theTask);
    secondPayload.setWorkbasketKey("anotherOne");
    assertThat(secondPayload).isNotEqualTo(theTask);
  }
}
