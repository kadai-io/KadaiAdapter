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

package io.kadai.adapter.camunda.mapper;

import java.text.SimpleDateFormat;
import tools.jackson.databind.json.JsonMapper;

/** This class is responsible for configuring the JsonMapper of Jackson. */
public final class JacksonConfigurator {

  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private JacksonConfigurator() {}

  public static JsonMapper createAndConfigureJsonMapper() {
    return JsonMapper.builder()
        .defaultDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT))
        .build();
  }
}
