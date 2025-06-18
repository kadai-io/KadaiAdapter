package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
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
    // io.camunda.zeebe:userTaskKey -> 2251799813782683 or job.getElementInstanceKey:
    // 2251799813782682 ?
    referencedTask.setManualPriority(
        customHeaders.get("io.camunda.zeebe:priority")); // todo: use variable instead?
    referencedTask.setAssignee(customHeaders.get("io.camunda.zeebe:assignee"));
    referencedTask.setDue(customHeaders.get("io.camunda.zeebe:dueDate"));
    referencedTask.setTaskDefinitionKey(job.getElementId());
    referencedTask.setBusinessProcessId(job.getBpmnProcessId());

    referencedTask.setWorkbasketKey(getVariable(job, "kadai.workbasket-key"));
    referencedTask.setWorkbasketKey(getVariable(job, "kadai.classification-key"));
    referencedTask.setWorkbasketKey(getVariable(job, "kadai.domain"));

    referencedTask.setCustomInt1(getVariable(job, "kadai.custom-int-1"));
    referencedTask.setCustomInt2(getVariable(job, "kadai.custom-int-2"));
    referencedTask.setCustomInt3(getVariable(job, "kadai.custom-int-3"));
    referencedTask.setCustomInt4(getVariable(job, "kadai.custom-int-4"));
    referencedTask.setCustomInt5(getVariable(job, "kadai.custom-int-5"));
    referencedTask.setCustomInt6(getVariable(job, "kadai.custom-int-6"));
    referencedTask.setCustomInt7(getVariable(job, "kadai.custom-int-7"));
    referencedTask.setCustomInt8(getVariable(job, "kadai.custom-int-8"));

    // todo: missing name, documentation and extension properties
    return referencedTask;
  }

  private String getVariable(ActivatedJob job, String variableName) {
    String variable = null;
    try {
      Object variableObj = job.getVariable(variableName);
      if (variableObj instanceof String variableAsString) {
        variable = variableAsString;
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve {} " + "for task {} in ProcessDefinition {}",
          variableName,
          job.getElementId(),
          job.getBpmnProcessId(),
          e);
    }
    return variable;
  }
}
