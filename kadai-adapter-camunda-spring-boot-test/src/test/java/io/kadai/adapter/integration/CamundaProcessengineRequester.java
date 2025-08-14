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

package io.kadai.adapter.integration;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Class to assist with building requests against the Camunda REST API. */
public class CamundaProcessengineRequester {

  private static final String BASIC_ENGINE_PATH = "/engine-rest/engine/";
  private static final String COMPLETE_TASK_PATH = "/complete";
  private static final String HISTORY_PATH = "/history";
  private static final String PROCESS_DEFINITION_KEY_PATH = "/process-definition/key/";
  private static final String PROCESS_DEFINITION_START_PATH = "/start";
  private static final String PROCESS_INSTANCE_PATH = "/process-instance";
  private static final String TASK_PATH = "/task";

  private final RestClient restClient;

  private final String processEngineKey;

  private final HttpHeaderProvider httpHeaderProvider;

  /**
   * Constructor for setting up a requester for a process engine with a key other than "default".
   *
   * @param processEngineKey the key of the camunda process engine to be called.
   * @param restClient the {@link RestClient} to be used for the REST calls.
   * @param httpHeaderProvider httpHeaderProvider to set correct headers on HTTP Request
   */
  public CamundaProcessengineRequester(
      String processEngineKey, RestClient restClient, HttpHeaderProvider httpHeaderProvider) {
    this.processEngineKey = processEngineKey;
    this.restClient = restClient;
    this.httpHeaderProvider = httpHeaderProvider;
  }

  /**
   * Default constructor to use the default process engine with its key "default".
   *
   * @param restClient the {@link RestClient} to be used for the REST calls.
   * @param httpHeaderProvider httpHeaderProvider to set correct headers on HTTP Request
   */
  public CamundaProcessengineRequester(
      RestClient restClient, HttpHeaderProvider httpHeaderProvider) {
    this("default", restClient, httpHeaderProvider);
  }

  /**
   * Starts an instance of the process with the given key in the process engine and returns its id.
   * Requires a process model of the given key to be already deployed within the process engine.
   *
   * @param processKey the key of the process to be started.
   * @param variables the variables passed at process start.
   * @return the internal id of the process instance.
   * @throws JSONException in case of JSON problems
   */
  public String startCamundaProcessAndReturnId(String processKey, String variables)
      throws JSONException {
    String url =
        BASIC_ENGINE_PATH
            + this.processEngineKey
            + PROCESS_DEFINITION_KEY_PATH
            + processKey
            + PROCESS_DEFINITION_START_PATH;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    String body = "{" + variables + "}";

    ResponseEntity<String> answer =
        restClient
            .post()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(body)
            .retrieve()
            .toEntity(String.class);
    JSONObject processJson = new JSONObject(answer.getBody());
    return (String) processJson.get("id");
  }

  /**
   * Retrieves the current tasks active in the current executions of a given process instance. Since
   * process instances may have several executions due to AND-Splits, several tasks may be active in
   * a given process instance.
   *
   * @param processInstanceId the id of the process instance for which tasks are to be retrieved.
   * @return a list of tasks currently active in the process instance. May be empty.
   * @throws JSONException in case of JSON problems
   */
  public List<String> getTaskIdsFromProcessInstanceId(String processInstanceId)
      throws JSONException {
    List<String> returnList = new ArrayList<String>();

    String url = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();

    ResponseEntity<String> answer =
        restClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(String.class);
    JSONArray tasklistJson = new JSONArray(answer.getBody());
    for (int i = 0; i < tasklistJson.length(); i++) {
      JSONObject taskJson = (JSONObject) tasklistJson.get(i);
      String taskId = (String) taskJson.get("id");
      String taskProcessInstanceId = (String) taskJson.get("processInstanceId");
      if (processInstanceId.equals(taskProcessInstanceId)) {
        returnList.add(taskId);
      }
    }
    return returnList;
  }

  /**
   * Completes the camunda task with the given Id. Returns true if successful.
   *
   * @param camundaTaskId the id of the task to be completed.
   * @return true if completion was successful. False on failure.
   * @throws JSONException in case of JSON problems
   */
  public boolean completeTaskWithId(String camundaTaskId) throws JSONException {
    String url =
        BASIC_ENGINE_PATH
            + this.processEngineKey
            + TASK_PATH
            + "/"
            + camundaTaskId
            + COMPLETE_TASK_PATH;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> answer =
        restClient
            .post()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body("{}")
            .retrieve()
            .toEntity(String.class);
    if (answer.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      // successful requests give no answer with a body
      return true;
    } else {
      JSONObject answerJson = new JSONObject(answer.getBody());
      String answerMessage = (String) answerJson.get("message");
      if (answerMessage.contains("Cannot complete task")) {
        return false;
      }
      return false;
    }
  }

  /**
   * Retrieves the camunda task with the given id. Returns {@code true} if successful.
   *
   * @param camundaTaskId the id of the task to be retrieved.
   * @return {@code true} if retrieval was successful, {@code false} if not.
   * @throws JSONException in case of JSON problems
   */
  public boolean getTaskFromTaskId(String camundaTaskId) throws JSONException {
    String url = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH + "/" + camundaTaskId;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> response;
    try {
      response =
          restClient
              .get()
              .uri(url)
              .headers(httpHeaders -> httpHeaders.addAll(headers))
              .retrieve()
              .toEntity(String.class);
    } catch (HttpClientErrorException.NotFound e) {
      response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getResponseBodyAsString());
    }

    JSONObject taskRetrievalAnswerJson = new JSONObject(response.getBody());
    return !(HttpStatus.NOT_FOUND.equals(response.getStatusCode())
        && ((String) taskRetrievalAnswerJson.get("message")).contains("No matching task with id")
        && "InvalidRequestException".equals(taskRetrievalAnswerJson.get("type")));
  }

  /**
   * Retrieves the camunda task with the given id from camundas task history. Returns {@code true}
   * if successful.
   *
   * @param camundaTaskId the id of the task to be retrieved.
   * @return {@code true} if retrieval was successful, {@code false} if not.
   * @throws JSONException in case of JSON problems
   */
  public boolean getTaskFromHistoryFromTaskId(String camundaTaskId) throws JSONException {
    String url =
        BASIC_ENGINE_PATH
            + this.processEngineKey
            + HISTORY_PATH
            + TASK_PATH
            + "/?taskId="
            + camundaTaskId;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> response =
        restClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(String.class);
    // no task found will only show in empty body
    JSONArray taskHistoryRetrievalAnswerJson = new JSONArray(response.getBody());
    if (taskHistoryRetrievalAnswerJson.isEmpty()) {
      return false;
    } else {
      String historyTaskId =
          (String) ((JSONObject) taskHistoryRetrievalAnswerJson.get(0)).get("id");
      return camundaTaskId.contentEquals(historyTaskId);
    }
  }

  public boolean isCorrectAssigneeFromHistory(String camundaTaskId, String assignee)
      throws JSONException {
    String url =
        BASIC_ENGINE_PATH
            + this.processEngineKey
            + HISTORY_PATH
            + TASK_PATH
            + "/?taskId="
            + camundaTaskId;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> response =
        restClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(String.class);
    // no task found will only show in empty body
    JSONArray taskHistoryRetrievalAnswerJson = new JSONArray(response.getBody());
    if (taskHistoryRetrievalAnswerJson.isEmpty()) {
      return false;
    } else {
      String camundaTaskAssignee =
          (String) ((JSONObject) taskHistoryRetrievalAnswerJson.get(0)).get("assignee");
      return assignee.equals(camundaTaskAssignee);
    }
  }

  /**
   * Deletes the camunda-process-instance with the given Id. Returns true if successful.
   *
   * @param processInstanceId the id of the process-instance to be delete.
   * @param skipCustomListeners a flag that specifies whether camunda listeners are to be skipped.
   * @return true if deletion was successful. False on failure.
   * @throws JSONException in case of JSON problems
   */
  public boolean deleteProcessInstanceWithId(String processInstanceId, boolean skipCustomListeners)
      throws JSONException {
    String url =
        BASIC_ENGINE_PATH + this.processEngineKey + PROCESS_INSTANCE_PATH + "/" + processInstanceId;
    if (skipCustomListeners) {
      url += "?skipCustomListeners=true";
    }
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> answer;
    try {
      answer =
          restClient
              .delete()
              .uri(url)
              .headers(httpHeaders -> httpHeaders.addAll(headers))
              .retrieve()
              .toEntity(String.class);
    } catch (HttpClientErrorException.NotFound e) {
      answer = ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getResponseBodyAsString());
    }
    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    } else {
      JSONObject answerJson = new JSONObject(answer.getBody());
      String answerMessage = (String) answerJson.get("message");
      if (answerMessage.contains("Process instance with id")
          && answerMessage.contains("does not exist")) {
        return false;
      }
    }
    return false;
  }

  /**
   * Determines if a provided assignee equals the assignee of a camunda task.
   *
   * @param assigneeValueToVerify a String of the assignee to verify
   * @param camundaTaskId the ID of the camunda task which will be checked for its assignee
   * @return true if the provided assignee equals the assignee of the camunda task, otherwise false
   */
  public boolean isCorrectAssignee(String camundaTaskId, String assigneeValueToVerify) {

    String requestUrl = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH + "/" + camundaTaskId;
    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
    ResponseEntity<String> responseEntity =
        restClient
            .get()
            .uri(requestUrl)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .retrieve()
            .toEntity(String.class);
    JSONObject taskRetrievalAnswerJson = new JSONObject(responseEntity.getBody());

    if (taskRetrievalAnswerJson.get("assignee") == JSONObject.NULL) {
      return assigneeValueToVerify == null;
    }

    String assignee = taskRetrievalAnswerJson.getString("assignee");
    return assignee.equals(assigneeValueToVerify);
  }
}
