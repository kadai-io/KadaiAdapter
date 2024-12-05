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

package io.kadai.adapter.camunda.outbox.rest.controller;

/** Collection of Url to Controller mappings. */
public final class Mapping {

  public static final String URL_EVENTS = "/events";
  public static final String URL_EVENT = "/{eventId}";
  public static final String URL_DELETE_EVENTS = "/delete-successful-events";
  public static final String URL_DECREASE_REMAINING_RETRIES =
      URL_EVENT + "/decrease-remaining-retries";
  public static final String URL_UNLOCK_EVENT = "/unlock-event" + URL_EVENT;
  public static final String DELETE_FAILED_EVENTS = "/delete-failed-events";
  public static final String URL_COUNT_FAILED_EVENTS = "/count";
  public static final String URL_CSRF = "/csrf";

  private Mapping() {}
}
