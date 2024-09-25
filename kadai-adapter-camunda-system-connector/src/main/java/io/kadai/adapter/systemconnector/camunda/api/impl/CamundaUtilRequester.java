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

package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** Util class for camunda requests used in multiple components of CamundaSystemConnectorImpl. */
public class CamundaUtilRequester {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaUtilRequester.class);

  public static boolean isTaskNotExisting(
      HttpHeaderProvider httpHeaderProvider,
      RestTemplate restTemplate,
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      String camundaTaskId) {

    StringBuilder requestUrlBuilder = new StringBuilder();

    String requestUrl =
        requestUrlBuilder
            .append(camundaSystemUrlInfo.getSystemRestUrl())
            .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
            .append(camundaTaskId)
            .toString();

    HttpEntity<Void> requestEntity = httpHeaderProvider.prepareNewEntityForCamundaRestApi();
    try {
      restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
    } catch (HttpStatusCodeException ex) {
      boolean isNotExisting = HttpStatus.NOT_FOUND.equals(ex.getStatusCode());
      if (isNotExisting) {
        LOGGER.debug("Camunda Task {} is not existing. Returning silently", camundaTaskId);
      }
      return isNotExisting;
    }
    return false;
  }
}
