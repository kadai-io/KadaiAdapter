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
import java.util.Date;

/** Test entity for a complex process variable. */
public class ProcessVariableTestObjectTwo implements Serializable {

  private String stringFieldObjectTwo;
  private int intFieldObjectTwo;
  private double doubleFieldObjectTwo;
  private boolean booleanFieldObjectTwo;
  private Date dateFieldObjectTwo;

  public ProcessVariableTestObjectTwo() {}

  public ProcessVariableTestObjectTwo(
      String stringFieldObjectTwo,
      int intFieldObjectTwo,
      double doubleFieldObjectTwo,
      boolean booleanFieldObjectTwo,
      Date dateFieldObjectTwo) {
    this.stringFieldObjectTwo = stringFieldObjectTwo;
    this.intFieldObjectTwo = intFieldObjectTwo;
    this.doubleFieldObjectTwo = doubleFieldObjectTwo;
    this.booleanFieldObjectTwo = booleanFieldObjectTwo;
    this.dateFieldObjectTwo = dateFieldObjectTwo;
  }

  public String getstringFieldObjectTwo() {
    return stringFieldObjectTwo;
  }

  public void setstringFieldObjectTwo(String stringFieldObjectTwo) {
    this.stringFieldObjectTwo = stringFieldObjectTwo;
  }

  public int getIntFieldObjectTwo() {
    return intFieldObjectTwo;
  }

  public void setIntFieldObjectTwo(int intFieldObjectTwo) {
    this.intFieldObjectTwo = intFieldObjectTwo;
  }

  public double getDoubleFieldObjectTwo() {
    return doubleFieldObjectTwo;
  }

  public void setDoubleFieldObjectTwo(double doubleFieldObjectTwo) {
    this.doubleFieldObjectTwo = doubleFieldObjectTwo;
  }

  public boolean isBooleanFieldObjectTwo() {
    return booleanFieldObjectTwo;
  }

  public void setBooleanFieldObjectTwo(boolean booleanFieldObjectTwo) {
    this.booleanFieldObjectTwo = booleanFieldObjectTwo;
  }

  public Date getDateFieldObjectTwo() {
    return dateFieldObjectTwo;
  }

  public void setDateFieldObjectTwo(Date dateFieldObjectTwo) {
    this.dateFieldObjectTwo = dateFieldObjectTwo;
  }
}
