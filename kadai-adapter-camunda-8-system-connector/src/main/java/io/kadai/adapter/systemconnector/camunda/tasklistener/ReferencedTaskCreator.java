package io.kadai.adapter.systemconnector.camunda.tasklistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Map<String, String> customHeaders = job.getCustomHeaders();

    referencedTask.setId(customHeaders.get("io.camunda.zeebe:userTaskKey"));
    // todo: io.camunda.zeebe:userTaskKey -> 2251799813782683 or
    //  job.getElementInstanceKey: 2251799813782682 ?
    referencedTask.setManualPriority(getVariable(job, "manual_priority"));
    referencedTask.setAssignee(customHeaders.get("io.camunda.zeebe:assignee"));
    referencedTask.setDue(customHeaders.get("io.camunda.zeebe:dueDate"));
    referencedTask.setTaskDefinitionKey(job.getElementId());
    referencedTask.setBusinessProcessId(job.getBpmnProcessId());

    referencedTask.setWorkbasketKey(getVariable(job, "workbasket_key"));
    referencedTask.setClassificationKey(getVariable(job, "classification_key"));
    referencedTask.setDomain(getVariable(job, "domain"));
    referencedTask.setName(getVariable(job, "name")); // todo: use fallback domain here?

    referencedTask.setCustomInt1(getVariable(job, "custom-int-1"));
    referencedTask.setCustomInt2(getVariable(job, "custom-int-2"));
    referencedTask.setCustomInt3(getVariable(job, "custom-int-3"));
    referencedTask.setCustomInt4(getVariable(job, "custom-int-4"));
    referencedTask.setCustomInt5(getVariable(job, "custom-int-5"));
    referencedTask.setCustomInt6(getVariable(job, "custom-int-6"));
    referencedTask.setCustomInt7(getVariable(job, "custom-int-7"));
    referencedTask.setCustomInt8(getVariable(job, "custom-int-8"));

    referencedTask.setVariables(getKadaiProcessVariables(job));

    LOGGER.debug("Creating ReferencedTask from job: {}", referencedTask);

    return referencedTask;
  }

  /**
   * Retrieves a variable from the ActivatedJob by its name, prefixed with "kadai_". If the variable
   * is not found or is empty, it returns null.
   *
   * @param job The ActivatedJob containing the task information.
   * @param variableName The name of the variable to retrieve.
   * @return The value of the variable as a String, or null if not found or empty.
   */
  private String getVariable(ActivatedJob job, String variableName) {
    try {
      Object variableObj = job.getVariable("kadai_" + variableName);
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
      String taskVariablesConcatenated = getVariable(job, "attributes");

      if (taskVariablesConcatenated != null) {
        variableNames = splitVariableNamesString(taskVariablesConcatenated);

      } else {
        return "{}";
      }

      Map<String, Object> variables = new HashMap<>();

      variableNames.forEach(
          nameOfVariableToAdd ->
              variables.put(nameOfVariableToAdd, job.getVariable(nameOfVariableToAdd)));

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
