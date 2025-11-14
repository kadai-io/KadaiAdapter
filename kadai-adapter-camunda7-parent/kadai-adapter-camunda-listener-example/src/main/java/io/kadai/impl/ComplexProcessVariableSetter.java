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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

/**
 * This class is responsible for setting complex variables containing Objects on a camunda user
 * task.
 */
public class ComplexProcessVariableSetter implements JavaDelegate {

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    Date date = dateFormatter.parse("01-01-1970 13:12:11");

    String specialCharactersString =
        "\fForm feed \b Backspace \t Tab \\Backslash \n newLine \r Carriage return \" DoubleQuote";
    ProcessVariableTestObjectTwo processVariableTestObjectTwo =
        new ProcessVariableTestObjectTwo("stringValueObjectTwo", 2, 2.2, true, date);
    ProcessVariableTestObject processVariableTestObject =
        new ProcessVariableTestObject(
            specialCharactersString, 1, 1.1, false, processVariableTestObjectTwo);

    delegateExecution.setVariable("attribute1", processVariableTestObject);
    delegateExecution.setVariable("attribute2", 5);
    delegateExecution.setVariable("attribute3", true);
  }
}
