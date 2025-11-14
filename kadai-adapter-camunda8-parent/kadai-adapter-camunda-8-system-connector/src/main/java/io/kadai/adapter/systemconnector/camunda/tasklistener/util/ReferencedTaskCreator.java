package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.UserTaskProperties;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReferencedTaskCreator {

  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(ReferencedTaskCreator.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Camunda8System camunda8System;

  @Autowired
  public ReferencedTaskCreator(Camunda8System camunda8System) {
    this.camunda8System = camunda8System;
  }

  /**
   * Creates a ReferencedTask from the given ActivatedJob. This method extracts various task-related
   * information from the job's custom headers and variables, and populates a ReferencedTask object.
   *
   * @param job The ActivatedJob containing the task information.
   * @return A ReferencedTask object populated with data from the job.
   */
  public ReferencedTask createReferencedTaskFromJob(ActivatedJob job) {
    ReferencedTask referencedTask = new ReferencedTask();
    UserTaskProperties userTaskProperties = job.getUserTask();
    referencedTask.setId(resolveTaskId(job, camunda8System));
    referencedTask.setManualPriority(getVariable(job, "kadai_manual_priority"));
    referencedTask.setAssignee(userTaskProperties.getAssignee());
    referencedTask.setDue(formatIso8601OffsetDateTime(userTaskProperties.getDueDate()));
    referencedTask.setPlanned(formatIso8601OffsetDateTime(userTaskProperties.getFollowUpDate()));
    referencedTask.setTaskDefinitionKey(job.getElementId());
    referencedTask.setBusinessProcessId(job.getBpmnProcessId());

    referencedTask.setWorkbasketKey(getVariable(job, "kadai_workbasket_key"));
    referencedTask.setClassificationKey(getVariable(job, "kadai_classification_key"));

    String domain = getVariable(job, "kadai_domain");
    referencedTask.setDomain(domain);

    String name = getVariable(job, "kadai_name");
    if (name == null || name.isBlank()) {
      name = domain;
    }
    referencedTask.setName(name);

    referencedTask.setCustomInt1(getVariable(job, "kadai_custom_int_1"));
    referencedTask.setCustomInt2(getVariable(job, "kadai_custom_int_2"));
    referencedTask.setCustomInt3(getVariable(job, "kadai_custom_int_3"));
    referencedTask.setCustomInt4(getVariable(job, "kadai_custom_int_4"));
    referencedTask.setCustomInt5(getVariable(job, "kadai_custom_int_5"));
    referencedTask.setCustomInt6(getVariable(job, "kadai_custom_int_6"));
    referencedTask.setCustomInt7(getVariable(job, "kadai_custom_int_7"));
    referencedTask.setCustomInt8(getVariable(job, "kadai_custom_int_8"));

    referencedTask.setVariables(getKadaiProcessVariables(job));
    referencedTask.setSystemUrl(camunda8System.getSystemUrl());

    LOGGER.debug("Creating ReferencedTask from job: {}", referencedTask);

    return referencedTask;
  }

  @SuppressWarnings("SameParameterValue")
  static List<String> splitVariableNames(String variableNamesConcatenated, char delimiter) {
    List<String> result = new LinkedList<>();
    variableNamesConcatenated = variableNamesConcatenated.trim();
    int length = variableNamesConcatenated.length();
    int start = 0;

    for (int i = 0; i < length; i++) {
      if (variableNamesConcatenated.charAt(i) == delimiter) {
        if (i > start) {
          result.add(variableNamesConcatenated.substring(start, i).trim());
        }
        start = i + 1;
      }
    }

    if (start < length) {
      result.add(variableNamesConcatenated.substring(start).trim());
    }

    return result;
  }

  private static String resolveTaskId(ActivatedJob activatedJob, Camunda8System camunda8System) {
    return String.format(
        "c8sysid-%d-utk-%d",
        camunda8System.getIdentifier(), activatedJob.getUserTask().getUserTaskKey());
  }

  /**
   * Retrieves a variable from the ActivatedJob by its name. If the variable is not found or is
   * empty, it returns null.
   *
   * @param job The ActivatedJob containing the task information.
   * @param variableName The name of the variable to retrieve.
   * @return The value of the variable as a String, or null if not found or empty.
   */
  private String getVariable(ActivatedJob job, String variableName) {
    try {
      Object variableObj = job.getVariablesAsMap().get(variableName);
      if (variableObj instanceof String variableAsString && !variableAsString.isBlank()) {
        return variableAsString;
      }
      if (variableObj instanceof Integer variableAsInt) {
        return String.valueOf(variableAsInt);
      }
    } catch (NullPointerException e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve '{}' for task '{}' in ProcessDefinition '{}'",
          variableName,
          job.getElementId(),
          job.getBpmnProcessId(),
          e);
    }
    return null; // Default value
  }

  /**
   * Retrieves the process variables for a Kadai task from the ActivatedJob. The variable names are
   * concatenated in the "kadai_attributes" process variable, which is expected to be a
   * comma-separated string.
   *
   * @param job The ActivatedJob containing the task information.
   * @return A JSON string representation of the process variables, or null if an error occurs.
   */
  private String getKadaiProcessVariables(ActivatedJob job) {

    try {

      List<String> variableNames;

      // Get Task Variables
      String taskVariablesConcatenated = getVariable(job, "kadai_attributes");

      if (taskVariablesConcatenated != null) {
        variableNames = splitVariableNames(taskVariablesConcatenated, ',');

      } else {
        return "{}";
      }

      Map<String, Object> variables = new HashMap<>();

      variableNames.forEach(
          nameOfVariableToAdd ->
              variables.put(nameOfVariableToAdd, getVariable(job, nameOfVariableToAdd)));

      return OBJECT_MAPPER.writeValueAsString(variables);

    } catch (JsonProcessingException e) {
      LOGGER.error(
          "Error while trying to retrieve variables for task '{}' in ProcessDefinition '{}'",
          job.getElementId(),
          job.getBpmnProcessId(),
          e);
    }
    return null;
  }

  /**
   * Formats given offset-date-time according to ISO8601.
   *
   * <p>Returns null if input is null.
   *
   * @param camundaDateTime Camunda-DateTime to format
   * @return formatted DateTime
   */
  private static String formatIso8601OffsetDateTime(OffsetDateTime camundaDateTime) {
    return camundaDateTime == null
        ? null
        : camundaDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
  }
}
