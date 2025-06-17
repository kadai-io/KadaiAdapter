package io.kadai.adapter.systemconnector.camunda.task.listener;

import io.camunda.client.api.response.ActivatedJob;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.Map;

public class ReferencedTaskCreator {

  public ReferencedTask createReferencedTaskFromJob(ActivatedJob job) {
    ReferencedTask task = new ReferencedTask();
    Map<String, String> customHeaders = job.getCustomHeaders();

    task.setId(customHeaders.get("io.camunda.zeebe:userTaskKey")); // or job.getElementInstanceKey?
    task.setPriority(customHeaders.get("io.camunda.zeebe:priority"));
    task.setAssignee(customHeaders.get("io.camunda.zeebe:assignee"));
    task.setDue(customHeaders.get("io.camunda.zeebe:dueDate"));
    // todo: owner, does not exist in C8! use assignee instead?
    task.setTaskDefinitionKey(job.getElementId()); // Is this correct?
    task.setBusinessProcessId(job.getBpmnProcessId());
    // todo: use variables for other attributes?
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
