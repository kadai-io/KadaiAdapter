package io.kadai.adapter.systemconnector.camunda;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8SystemConnectorImpl;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8TaskClaimCanceler;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8TaskClaimer;
import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8TaskCompleter;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class Camunda8TestSetupListener implements TestExecutionListener {

  @Override
  public void beforeTestMethod(TestContext testContext) {
    CamundaClient client = testContext.getApplicationContext().getBean(CamundaClient.class);
    Camunda8System camunda8System =
        testContext.getApplicationContext().getBean(Camunda8System.class);

    final String camunda8RestAddress = client.getConfiguration().getRestAddress().toString();
    camunda8System.setRestAddress(camunda8RestAddress);

    // Each test-suite gets its own Camunda8-Instance
    // Hence, why we keep adding these new instances
    // While this technically leads to "old" OutBoundSystemConnectors it's irrelevant
    // because we look up by key (system-url) anyway
    AdapterManager adapterManager =
        testContext.getApplicationContext().getBean(AdapterManager.class);
    final Camunda8SystemConnectorImpl camunda8SystemConnectorImpl =
        new Camunda8SystemConnectorImpl(
            testContext.getApplicationContext().getBean(Camunda8System.class),
            testContext.getApplicationContext().getBean(Camunda8TaskClaimer.class),
            testContext.getApplicationContext().getBean(Camunda8TaskCompleter.class),
            testContext.getApplicationContext().getBean(Camunda8TaskClaimCanceler.class));

    adapterManager
        .getOutboundSystemConnectors()
        .put(camunda8RestAddress, camunda8SystemConnectorImpl);
  }
}
