package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReferencedTaskCreator {

  public ReferencedTask createReferencedTaskFromJob(ActivatedJob job) {
    ReferencedTask task = new ReferencedTask(); // No variables are set
    // custom headers: userTaskKey, assignee, formKey, priority, action
    // how to get name?
    Map<String, String> customHeaders = job.getCustomHeaders();

    task.setId(customHeaders.get("io.camunda.zeebe:userTaskKey"));
    // io.camunda.zeebe:userTaskKey -> 2251799813782683 or job.getElementInstanceKey:
    // 2251799813782682 ?
    task.setManualPriority(
        customHeaders.get(
            "io.camunda.zeebe:priority")); // todo: should not use this because it is the default
    // priority
    task.setAssignee(customHeaders.get("io.camunda.zeebe:assignee"));
    task.setDue(customHeaders.get("io.camunda.zeebe:dueDate"));
    task.setTaskDefinitionKey(job.getElementId());
    task.setBusinessProcessId(job.getBpmnProcessId());
    // todo: use variables for custom attributes
    return fetchDetailsFromTaskApi(task);
  }

  private ReferencedTask fetchDetailsFromTaskApi(ReferencedTask task) {
    // Logic to fetch task details from theAPI using the taskId
    // Will be able to use CamundaClient for this:
    // https://forum.camunda.io/t/tasklist-api-and-zeebeclient/61011
    // This is a placeholder for the actual implementation

    // fetch details like planned, name, description, etc.
    return task;
  }
}
