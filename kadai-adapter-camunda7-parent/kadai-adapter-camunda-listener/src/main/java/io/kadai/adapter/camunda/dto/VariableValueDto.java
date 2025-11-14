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

import java.util.Map;

/** This class represents a DTO which contains a process variable and its info. */
public class VariableValueDto {

  protected String type;

  protected Object value;

  protected Map<String, Object> valueInfo;

  public VariableValueDto() {}

  public VariableValueDto(String type, Object value, Map<String, Object> valueInfo) {
    this.type = type;
    this.value = value;
    this.valueInfo = valueInfo;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public Map<String, Object> getValueInfo() {
    return valueInfo;
  }

  public void setValueInfo(Map<String, Object> valueInfo) {
    this.valueInfo = valueInfo;
  }
}
