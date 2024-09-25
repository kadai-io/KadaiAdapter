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

package io.kadai.adapter.camunda.util;

import java.util.HashMap;
import java.util.Map;

public class PrimitiveWrapperChecker {

  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_TYPE_MAP = new HashMap<>(8);

  static {
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, Boolean.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, Byte.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, Character.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, Double.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, Float.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, Integer.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, Long.TYPE);
    PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, Short.TYPE);
  }

  private PrimitiveWrapperChecker() {
    // do not allow initalization
  }

  public static boolean isPrimitiveWrapper(Class<?> clazz) {
    return PRIMITIVE_WRAPPER_TYPE_MAP.containsKey(clazz);
  }
}
