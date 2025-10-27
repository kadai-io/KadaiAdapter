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

package io.kadai.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.common.api.KadaiEngine;
import io.kadai.spi.routing.api.TaskRoutingProvider;
import io.kadai.spi.routing.internal.TaskRoutingManager;
import io.kadai.taskrouting.ExampleTaskRouter;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = KadaiAdapterApplication.class)
class KadaiAdapterApplicationTest {

  private final List<TaskRoutingProvider> taskRoutingProviders;

  KadaiAdapterApplicationTest(@Autowired KadaiEngine kadaiEngine) throws Exception {
    TaskRoutingManager taskRoutingManager =
        (TaskRoutingManager) getValueFromPrivateFieldOfSuperclass(kadaiEngine,
            "taskRoutingManager");
    this.taskRoutingProviders =
        (List<TaskRoutingProvider>)
            getValueFromPrivateField(taskRoutingManager, "taskRoutingProviders");
  }

  @Test
  void should_AutowireExampleTaskRouter_When_ApplicationIsStarting() {
    assertThat(taskRoutingProviders).isNotNull().hasSize(1);
    assertThat(taskRoutingProviders.get(0)).isInstanceOf(ExampleTaskRouter.class);
  }

  private Object getValueFromPrivateField(Object obj, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field nameField = obj.getClass().getDeclaredField(fieldName);
    nameField.setAccessible(true);

    return nameField.get(obj);
  }

  private Object getValueFromPrivateFieldOfSuperclass(Object obj, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field nameField = obj.getClass().getSuperclass().getDeclaredField(fieldName);
    nameField.setAccessible(true);

    return nameField.get(obj);
  }
}
