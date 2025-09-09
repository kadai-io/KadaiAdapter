package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.UserTaskProperties;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReferencedTaskCreator {

  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(ReferencedTaskCreator.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    referencedTask.setId(String.valueOf(userTaskProperties.getUserTaskKey()));
    referencedTask.setManualPriority(getVariable(job, "kadai_manual_priority"));
    referencedTask.setAssignee(userTaskProperties.getAssignee());
    referencedTask.setDue(
        ZonedDateTime.parse(userTaskProperties.getDueDate())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
    referencedTask.setPlanned(
        ZonedDateTime.parse(userTaskProperties.getFollowUpDate())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
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

    // todo: add systemURL (https://github.com/kadai-io/KadaiAdapter/issues/149)

    LOGGER.debug("Creating ReferencedTask from job: {}", referencedTask);

    return referencedTask;
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
        variableNames = splitVariableNamesString(taskVariablesConcatenated);

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

  private List<String> splitVariableNamesString(String variableNamesConcatenated) {
    return Arrays.asList(variableNamesConcatenated.trim().split("\\s*,\\s*"));
  }
}
