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

package io.kadai.adapter.camunda.outbox.rest.model;

import java.io.Serializable;

/** POJO that represents an event in the camunda outbox table. */
public class CamundaTaskEvent implements Serializable {

  private int id;
  private String type;
  private String created;
  private String payload;
  private int remainingRetries;
  private String blockedUntil;
  private String error;
  private String camundaTaskId;
  private String systemEngineIdentifier;
  private String lockExpiresAt;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public int getRemainingRetries() {
    return remainingRetries;
  }

  public void setRemainingRetries(int remainingRetries) {
    this.remainingRetries = remainingRetries;
  }

  public String getBlockedUntil() {
    return blockedUntil;
  }

  public void setBlockedUntil(String blockedUntil) {
    this.blockedUntil = blockedUntil;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getCamundaTaskId() {
    return camundaTaskId;
  }

  public void setCamundaTaskId(String camundaTaskId) {
    this.camundaTaskId = camundaTaskId;
  }

  public String getSystemEngineIdentifier() {
    return systemEngineIdentifier;
  }

  public void setSystemEngineIdentifier(String systemEngineIdentifier) {
    this.systemEngineIdentifier = systemEngineIdentifier;
  }

  public String getLockExpiresAt() {
    return lockExpiresAt;
  }

  public void setLockExpiresAt(String lockExpiresAt) {
    this.lockExpiresAt = lockExpiresAt;
  }

  @Override
  public String toString() {
    return "CamundaTaskEvent [id="
        + id
        + ", type="
        + type
        + ", created="
        + created
        + ", payload="
        + payload
        + ", remainingRetries="
        + remainingRetries
        + ", blockedUntil="
        + blockedUntil
        + ", error="
        + error
        + ", camundaTaskId="
        + camundaTaskId
        + ", systemEngineIdentifier="
        + systemEngineIdentifier
        + ", lockExpiresAt="
        + lockExpiresAt
        + "]";
  }
}
