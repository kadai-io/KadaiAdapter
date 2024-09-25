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

package io.kadai.adapter.exceptions;

import io.kadai.common.api.exceptions.ErrorCode;
import io.kadai.common.api.exceptions.KadaiRuntimeException;

/** This exception is thrown when an assertion is violated. */
public class AssertionViolationException extends KadaiRuntimeException {

  public static final String ERROR_KEY = "ASSERTION_FAILED";

  public AssertionViolationException(String message) {
    super(message, ErrorCode.of(ERROR_KEY));
  }
}
