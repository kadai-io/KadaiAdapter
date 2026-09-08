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

package io.kadai.adapter.systemconnector.camunda.dto;

/** Payload of a Camunda 7 task event. */
public class Camunda7ReferencedTaskDto {

  private String id;
  private String name;
  private String assignee;
  private String created;
  private String planned;
  private String due;
  private String description;
  private String owner;
  private String priority;
  private String manualPriority;
  private String taskDefinitionKey;
  private String businessProcessId;
  private String variables;
  private String taskState;
  private String domain;
  private String classificationKey;
  private String workbasketKey;
  private String customInt1;
  private String customInt2;
  private String customInt3;
  private String customInt4;
  private String customInt5;
  private String customInt6;
  private String customInt7;
  private String customInt8;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getPlanned() {
    return planned;
  }

  public void setPlanned(String planned) {
    this.planned = planned;
  }

  public String getDue() {
    return due;
  }

  public void setDue(String due) {
    this.due = due;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getManualPriority() {
    return manualPriority;
  }

  public void setManualPriority(String manualPriority) {
    this.manualPriority = manualPriority;
  }

  public String getTaskDefinitionKey() {
    return taskDefinitionKey;
  }

  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  public String getBusinessProcessId() {
    return businessProcessId;
  }

  public void setBusinessProcessId(String businessProcessId) {
    this.businessProcessId = businessProcessId;
  }

  public String getVariables() {
    return variables;
  }

  public void setVariables(String variables) {
    this.variables = variables;
  }

  public String getTaskState() {
    return taskState;
  }

  public void setTaskState(String taskState) {
    this.taskState = taskState;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getClassificationKey() {
    return classificationKey;
  }

  public void setClassificationKey(String classificationKey) {
    this.classificationKey = classificationKey;
  }

  public String getWorkbasketKey() {
    return workbasketKey;
  }

  public void setWorkbasketKey(String workbasketKey) {
    this.workbasketKey = workbasketKey;
  }

  public String getCustomInt1() {
    return customInt1;
  }

  public void setCustomInt1(String customInt1) {
    this.customInt1 = customInt1;
  }

  public String getCustomInt2() {
    return customInt2;
  }

  public void setCustomInt2(String customInt2) {
    this.customInt2 = customInt2;
  }

  public String getCustomInt3() {
    return customInt3;
  }

  public void setCustomInt3(String customInt3) {
    this.customInt3 = customInt3;
  }

  public String getCustomInt4() {
    return customInt4;
  }

  public void setCustomInt4(String customInt4) {
    this.customInt4 = customInt4;
  }

  public String getCustomInt5() {
    return customInt5;
  }

  public void setCustomInt5(String customInt5) {
    this.customInt5 = customInt5;
  }

  public String getCustomInt6() {
    return customInt6;
  }

  public void setCustomInt6(String customInt6) {
    this.customInt6 = customInt6;
  }

  public String getCustomInt7() {
    return customInt7;
  }

  public void setCustomInt7(String customInt7) {
    this.customInt7 = customInt7;
  }

  public String getCustomInt8() {
    return customInt8;
  }

  public void setCustomInt8(String customInt8) {
    this.customInt8 = customInt8;
  }
}
