package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.kadai.adapter.impl.KadaiTaskStarter;

public class UserTaskCreation {

  private final KadaiTaskStarter taskStarter;

  public UserTaskCreation(KadaiTaskStarter taskStarter) {
    this.taskStarter = taskStarter;
  }

  @JobWorker(type = "user-task-listener-assigning") // this is not working for some reason
  public void receiveTaskCreatedEvent(final JobClient jobClient, final ActivatedJob job) {
    // Logic to handle task creation event
    // This method will be called when a new user task is created in Camunda 8
  }
}
