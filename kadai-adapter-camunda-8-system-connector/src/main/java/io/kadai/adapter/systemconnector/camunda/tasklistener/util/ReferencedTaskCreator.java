package io.kadai.adapter.systemconnector.camunda.tasklistener.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.UserTaskProperties;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReferencedTaskCreator {

  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(ReferencedTaskCreator.class);

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
    Map<String, Object> variablesObj = job.getVariablesAsMap();
    Map<String,String> variables = variablesObj.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));

    referencedTask.setId(String.valueOf(job.getKey()));
    // todo: io.camunda.zeebe:userTaskKey -> 2251799813782683 or
    //  job.getElementInstanceKey: 2251799813782682 ?
    referencedTask.setManualPriority(variables.get("kadai_manual_priority"));
    referencedTask.setAssignee(userTaskProperties.getAssignee());
    referencedTask.setDue(userTaskProperties.getDueDate());
    referencedTask.setTaskDefinitionKey(job.getElementId());
    referencedTask.setBusinessProcessId(job.getBpmnProcessId());

    referencedTask.setWorkbasketKey(variables.get("kadai_workbasket_key"));
    referencedTask.setClassificationKey(variables.get("kadai_classification_key"));
    referencedTask.setDomain(variables.get("kadai_domain"));
    referencedTask.setName(variables.get("kadai_name")); // todo: use fallback domain here?

    referencedTask.setCustomInt1(variables.get("kadai_custom_int_1"));
    referencedTask.setCustomInt2(variables.get("kadai_custom_int_2"));
    referencedTask.setCustomInt3(variables.get("kadai_custom_int_3"));
    referencedTask.setCustomInt4(variables.get("kadai_custom_int_4"));
    referencedTask.setCustomInt5(variables.get("kadai_custom_int_5"));
    referencedTask.setCustomInt6(variables.get("kadai_custom_int_6"));
    referencedTask.setCustomInt7(variables.get("kadai_custom_int_7"));
    referencedTask.setCustomInt8(variables.get("kadai_custom_int_8"));

    referencedTask.setVariables(getKadaiProcessVariables(job));

    LOGGER.debug("Creating ReferencedTask from job: {}", referencedTask);

    return referencedTask;
  }

  private String getVariableWithKadaiPrefix(ActivatedJob job, String variableName) {
    return getVariable(job, "kadai_" + variableName);
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
    } catch (Exception e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve {} for task {} in ProcessDefinition {}",
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
      String taskVariablesConcatenated = getVariableWithKadaiPrefix(job, "attributes");

      if (taskVariablesConcatenated != null) {
        variableNames = splitVariableNamesString(taskVariablesConcatenated);

      } else {
        return "{}";
      }

      Map<String, Object> variables = new HashMap<>();

      variableNames.forEach(
          nameOfVariableToAdd ->
              variables.put(nameOfVariableToAdd, getVariable(job, nameOfVariableToAdd)));

      ObjectMapper objectMapper = new ObjectMapper();

      return objectMapper.writeValueAsString(variables);

    } catch (JsonProcessingException e) {
      LOGGER.error(
          "Error while trying to retrieve variables for task {} in ProcessDefinition {}",
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
