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

package io.kadai.adapter.util;

import io.kadai.adapter.exceptions.AssertionViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** utility class that allows to assert specific conditions. */
public final class Assert {

  private static final Logger LOGGER = LoggerFactory.getLogger(Assert.class);

  private Assert() {}

  public static void assertion(boolean isCondition, String condition) {
    if (!isCondition) {
      String assertion = "Assertion violation !(" + condition + ") ";
      LOGGER.error(assertion);
      throw new AssertionViolationException(assertion);
    }
  }
}
