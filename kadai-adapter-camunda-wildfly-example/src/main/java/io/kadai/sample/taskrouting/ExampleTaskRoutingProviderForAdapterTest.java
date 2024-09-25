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

package io.kadai.sample.taskrouting;

import io.kadai.common.api.KadaiEngine;
import io.kadai.spi.routing.api.TaskRoutingProvider;
import io.kadai.task.api.models.Task;
import io.kadai.workbasket.api.models.WorkbasketSummary;
import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class ExampleTaskRoutingProviderForAdapterTest implements TaskRoutingProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExampleTaskRoutingProviderForAdapterTest.class);

  private KadaiEngine theEngine;

  @Value("${kadai.sample.taskrouter.random:false}")
  private String routeRandomly;

  @Override
  public void initialize(KadaiEngine kadaiEngine) {
    theEngine = kadaiEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("true".equalsIgnoreCase(routeRandomly)) {
      return determineRandomWorkbasket();
    } else {
      return "WBI:100000000000000000000000000000000001";
    }
  }

  private String determineRandomWorkbasket() {
    List<WorkbasketSummary> wbs = theEngine.getWorkbasketService().createWorkbasketQuery().list();
    if (wbs != null && !(wbs.isEmpty())) {
      // select a random workbasket
      SecureRandom random = new SecureRandom();
      int n = random.nextInt(wbs.size());
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(String.format("ExampleTaskRoutingProvider Routs to %s", wbs.get(n)));
      }
      return wbs.get(n).getId();
    } else {
      return null;
    }
  }
}
