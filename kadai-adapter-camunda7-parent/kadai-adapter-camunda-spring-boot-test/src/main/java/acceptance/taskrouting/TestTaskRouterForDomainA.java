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

package acceptance.taskrouting;

import io.kadai.common.api.KadaiEngine;
import io.kadai.spi.routing.api.TaskRoutingProvider;
import io.kadai.task.api.models.Task;
import io.kadai.workbasket.api.models.WorkbasketSummary;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a sample implementation of TaskRouter. */
public class TestTaskRouterForDomainA implements TaskRoutingProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskRouterForDomainA.class);

  private KadaiEngine theEngine;

  @Override
  public void initialize(KadaiEngine kadaiEngine) {
    theEngine = kadaiEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("DOMAIN_A".equals(task.getDomain())) {
      List<WorkbasketSummary> wbs =
          theEngine.getWorkbasketService().createWorkbasketQuery().domainIn("DOMAIN_A").list();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(String.format("TestTaskRouterForDomainA Routing to %s", wbs.get(0)));
      }
      return wbs.get(0).getId();
    } else {
      return null;
    }
  }
}
