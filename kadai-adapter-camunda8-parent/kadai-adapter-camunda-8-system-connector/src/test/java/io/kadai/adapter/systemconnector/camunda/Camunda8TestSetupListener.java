package io.kadai.adapter.systemconnector.camunda;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * TestExecutionListener that sets Camunda8System.clusterApiUrl from the CamundaClient before each
 * test. Temporary workaround to camunda/camunda#40223
 */
public class Camunda8TestSetupListener implements TestExecutionListener {

  @Override
  public void beforeTestMethod(TestContext testContext) {
    CamundaClient client = testContext.getApplicationContext().getBean(CamundaClient.class);
    Camunda8System camunda8System =
        testContext.getApplicationContext().getBean(Camunda8System.class);

    if (client.getConfiguration().getRestAddress() != null) {
      camunda8System.setClusterApiUrl(client.getConfiguration().getRestAddress().toString());
    }
  }
}
