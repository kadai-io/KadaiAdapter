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

  public ReferencedTask createReferencedTaskFromJob(ActivatedJob job) {
    ReferencedTask referencedTask = new ReferencedTask();
    Map<String, String> customHeaders = job.getCustomHeaders();

    referencedTask.setId(customHeaders.get("io.camunda.zeebe:userTaskKey"));
    // todo: io.camunda.zeebe:userTaskKey -> 2251799813782683 or
    //  job.getElementInstanceKey: 2251799813782682 ?
    referencedTask.setManualPriority(
        customHeaders.get("io.camunda.zeebe:priority")); // todo: use variable instead?
    referencedTask.setAssignee(customHeaders.get("io.camunda.zeebe:assignee"));
    referencedTask.setDue(customHeaders.get("io.camunda.zeebe:dueDate"));
    referencedTask.setTaskDefinitionKey(job.getElementId());
    referencedTask.setBusinessProcessId(job.getBpmnProcessId());

    referencedTask.setWorkbasketKey(getVariable(job, "workbasket-key"));
    referencedTask.setWorkbasketKey(getVariable(job, "classification-key"));
    referencedTask.setWorkbasketKey(getVariable(job, "domain"));

    referencedTask.setCustomInt1(getVariable(job, "custom-int-1"));
    referencedTask.setCustomInt2(getVariable(job, "custom-int-2"));
    referencedTask.setCustomInt3(getVariable(job, "custom-int-3"));
    referencedTask.setCustomInt4(getVariable(job, "custom-int-4"));
    referencedTask.setCustomInt5(getVariable(job, "custom-int-5"));
    referencedTask.setCustomInt6(getVariable(job, "custom-int-6"));
    referencedTask.setCustomInt7(getVariable(job, "custom-int-7"));
    referencedTask.setCustomInt8(getVariable(job, "custom-int-8"));

    referencedTask.setVariables(getKadaiProcessVariables(job));

    // todo: missing name
    // todo: test!!
    return referencedTask;
  }

  private String getVariable(ActivatedJob job, String variableName) {
    try {
      Object variableObj = job.getVariable("kadai." + variableName);
      if (variableObj instanceof String variableAsString && !variableAsString.isBlank()) {
        return variableAsString;
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

  // todo: Documentation!

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
      String json = objectMapper.writeValueAsString(variables);

      if (!json.isEmpty()) {
        json = "{" + variables + "}";
      } else {
        return "{}";
      }

      return json;
    } catch (JsonProcessingException e) {
      LOGGER.error(
          "Error while trying to retrieve variables for task {} in ProcessDefinition {}",
          job.getElementId(),
          job.getBpmnProcessId(),
          e);}
    return null;
  }

  private List<String> splitVariableNamesString(String variableNamesConcatenated) {
    return Arrays.asList(variableNamesConcatenated.trim().split("\\s*,\\s*"));
  }
}
