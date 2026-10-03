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

package io.kadai.adapter.systemconnector.api;

import io.kadai.task.api.TaskState;
import java.util.Objects;

/** POJO that represents the core-relevant properties of a task in an external system. */
public class ReferencedTask {

  /** Connector-defined opaque identifier. The adapter core must not interpret its format. */
  private String id;

  private String name;
  private String assignee;
  private String created;
  private String due;
  private String planned;
  private String description;
  private String manualPriority;

  /** URL used by the adapter core to select the connector responsible for this task. */
  private String systemUrl;

  private String taskDefinitionKey;
  private String businessProcessId;
  private String variables;
  private TaskState taskState;
  // extension properties
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

  public String getBusinessProcessId() {
    return businessProcessId;
  }

  public void setBusinessProcessId(String businessProcessId) {
    this.businessProcessId = businessProcessId;
  }

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

  public String getManualPriority() {
    return manualPriority;
  }

  public void setManualPriority(String manualPriority) {
    this.manualPriority = manualPriority;
  }

  public String getSystemUrl() {
    return systemUrl;
  }

  public void setSystemUrl(String systemUrl) {
    this.systemUrl = systemUrl;
  }

  public String getTaskDefinitionKey() {
    return taskDefinitionKey;
  }

  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  public TaskState getTaskState() {
    return taskState;
  }

  public void setTaskState(TaskState taskState) {
    this.taskState = taskState;
  }

  public String getVariables() {
    return variables;
  }

  public void setVariables(String variables) {
    this.variables = variables;
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

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        name,
        assignee,
        created,
        due,
        description,
        manualPriority,
        systemUrl,
        taskDefinitionKey,
        businessProcessId,
        variables,
        taskState,
        domain,
        classificationKey,
        workbasketKey,
        customInt1,
        customInt2,
        customInt3,
        customInt4,
        customInt5,
        customInt6,
        customInt7,
        customInt8);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ReferencedTask other = (ReferencedTask) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(name, other.name)
        && Objects.equals(assignee, other.assignee)
        && Objects.equals(created, other.created)
        && Objects.equals(due, other.due)
        && Objects.equals(description, other.description)
        && Objects.equals(manualPriority, other.manualPriority)
        && Objects.equals(systemUrl, other.systemUrl)
        && Objects.equals(taskDefinitionKey, other.taskDefinitionKey)
        && Objects.equals(businessProcessId, other.businessProcessId)
        && Objects.equals(variables, other.variables)
        && Objects.equals(taskState, other.taskState)
        && Objects.equals(domain, other.domain)
        && Objects.equals(classificationKey, other.classificationKey)
        && Objects.equals(workbasketKey, other.workbasketKey)
        && Objects.equals(customInt1, other.customInt1)
        && Objects.equals(customInt2, other.customInt2)
        && Objects.equals(customInt3, other.customInt3)
        && Objects.equals(customInt4, other.customInt4)
        && Objects.equals(customInt5, other.customInt5)
        && Objects.equals(customInt6, other.customInt6)
        && Objects.equals(customInt7, other.customInt7)
        && Objects.equals(customInt8, other.customInt8);
  }

  @Override
  public String toString() {
    return "ReferencedTask [id="
        + id
        + ", name="
        + name
        + ", assignee="
        + assignee
        + ", created="
        + created
        + ", due="
        + due
        + ", description="
        + description
        + ", manualPriority="
        + manualPriority
        + ", systemUrl="
        + systemUrl
        + ", taskDefinitionKey="
        + taskDefinitionKey
        + ", businessProcessId="
        + businessProcessId
        + ", variables="
        + variables
        + ", taskState="
        + taskState
        + ", domain="
        + domain
        + ", classificationKey="
        + classificationKey
        + ", workbasketKey="
        + workbasketKey
        + ", customInt1="
        + customInt1
        + ", customInt2="
        + customInt2
        + ", customInt3="
        + customInt3
        + ", customInt4="
        + customInt4
        + ", customInt5="
        + customInt5
        + ", customInt6="
        + customInt6
        + ", customInt7="
        + customInt7
        + ", customInt8="
        + customInt8
        + "]";
  }
}
