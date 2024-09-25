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

package io.kadai.impl;

import java.io.Serializable;

/** Test entity for a complex process variable. */
public class ProcessVariableTestObject implements Serializable {

  private String stringField;
  private int intField;
  private double doubleField;
  private boolean booleanField;
  private ProcessVariableTestObjectTwo processVariableTestObjectTwoField;

  public ProcessVariableTestObject() {}

  public ProcessVariableTestObject(
      String stringField,
      int intField,
      double doubleField,
      boolean booleanField,
      ProcessVariableTestObjectTwo processVariableTestObjectTwoField) {
    this.stringField = stringField;
    this.intField = intField;
    this.doubleField = doubleField;
    this.booleanField = booleanField;
    this.processVariableTestObjectTwoField = processVariableTestObjectTwoField;
  }

  public String getStringField() {
    return stringField;
  }

  public void setStringField(String stringField) {
    this.stringField = stringField;
  }

  public int getIntField() {
    return intField;
  }

  public void setIntField(int intField) {
    this.intField = intField;
  }

  public double getDoubleField() {
    return doubleField;
  }

  public void setDoubleField(double doubleField) {
    this.doubleField = doubleField;
  }

  public boolean isBooleanField() {
    return booleanField;
  }

  public void setBooleanField(boolean booleanField) {
    this.booleanField = booleanField;
  }

  public ProcessVariableTestObjectTwo getProcessVariableTestObjectTwoField() {
    return processVariableTestObjectTwoField;
  }

  public void setProcessVariableTestObjectTwoField(
      ProcessVariableTestObjectTwo processVariableTestObjectTwoField) {
    this.processVariableTestObjectTwoField = processVariableTestObjectTwoField;
  }
}
