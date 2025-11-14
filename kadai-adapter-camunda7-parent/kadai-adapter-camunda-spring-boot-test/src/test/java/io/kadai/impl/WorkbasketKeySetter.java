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

import java.util.List;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for setting the String variable "kadai.workbasket-key" in a test
 * scenario.
 */
public class WorkbasketKeySetter implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkbasketKeySetter.class);

  @Override
  public void execute(DelegateExecution execution) {
    String currentDomainName = getDomainName(execution);
    if ("DOMAIN_A".equals(currentDomainName)) {
      LOGGER.info("Domain = DOMAIN_A --> Setting workbasket key to GPK_KSC ... ");
      execution.setVariable("kadai.workbasket-key", "GPK_KSC");
    } else if ("DOMAIN_B".equals(currentDomainName)) {
      LOGGER.info("Domain = DOMAIN_B --> Setting workbasket key to GPK_B_KSC ... ");
      execution.setVariable("kadai.workbasket-key", "GPK_B_KSC");
    } else {
      LOGGER.info("Found neither DOMAIN_A nor DOMAIN_B ... ");
    }
  }

  private String getDomainName(DelegateExecution execution) {
    String domainName = null;

    BpmnModelInstance model = execution.getBpmnModelInstance();

    try {
      List<CamundaProperty> processModelExtensionProperties =
          model.getModelElementsByType(CamundaProperty.class).stream()
              .filter(camundaProperty -> camundaProperty.getCamundaName().equals("kadai.domain"))
              .collect(Collectors.toList());

      if (processModelExtensionProperties.isEmpty()) {
        return domainName;
      } else {
        domainName = processModelExtensionProperties.get(0).getCamundaValue();
      }

    } catch (Exception e) {
      LOGGER.warn(
          "Caught while trying to retrieve the kadai.domain property from a process model", e);
    }

    return domainName;
  }
}
