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

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** encapsulate a response from the external system. */
public class SystemResponse {
  private final HttpStatus statusCode;
  private final Throwable throwable;

  public SystemResponse(int statusCode, Throwable throwable) {
    this(HttpStatus.resolve(statusCode), throwable);
  }

  public SystemResponse(HttpStatusCode statusCode, Throwable throwable) {
    this(HttpStatus.resolve(statusCode.value()), throwable);
  }

  public SystemResponse(HttpStatus statusCode, Throwable throwable) {
    this.statusCode = statusCode;
    this.throwable = throwable;
  }

  public HttpStatus getStatusCode() {
    return statusCode;
  }

  public Throwable getThrowable() {
    return throwable;
  }
}
