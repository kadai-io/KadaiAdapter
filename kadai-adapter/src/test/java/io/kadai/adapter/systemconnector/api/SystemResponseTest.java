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

package io.kadai.adapter.systemconnector.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

class SystemResponseTest {

  @Test
  void should_MapIntValueToCorrectHttpStatus_When_RelatedSystemResponseConstructorIsCalled() {
    assertThat(new SystemResponse(200, null).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void should_MapHttpStatusCodeToCorrectHttpStatus_When_RelatedSystemResponseConstructorIsCalled() {
    assertThat(new SystemResponse(HttpStatusCode.valueOf(200), null).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }
}
