package io.kadai.adapter.systemconnector.camunda;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.OutboundSystemConnector;
import io.kadai.adapter.systemconnector.camunda.config.Camunda8System;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class Camunda8TestSetupListener implements TestExecutionListener {

  private static final String CAMUNDA_8_SELF_MANAGED_DEFAULT_REST_ADDRESS = "http://localhost:8088";

  @Override
  public void beforeTestMethod(TestContext testContext) {
    CamundaClient client = testContext.getApplicationContext().getBean(CamundaClient.class);
    Camunda8System camunda8System =
        testContext.getApplicationContext().getBean(Camunda8System.class);
    AdapterManager adapterManager =
        testContext.getApplicationContext().getBean(AdapterManager.class);

    final String camunda8RestAddress = client.getConfiguration().getRestAddress().toString();
    camunda8System.setRestAddress(camunda8RestAddress);

    // When AdapterManager loads C8 SPI, the rest-address is not yet set for tests because
    // the CamundaClient is not yet initialized.
    // Therefore, Camunda itself defaults to CAMUNDA_8_SELF_MANAGED_DEFAULT_REST_ADDRESS.
    // Hence, why we need to overwrite it here with the actual address (due to dynamic port).
    final OutboundSystemConnector camunda8OutboundSystemConnector =
        adapterManager
            .getOutboundSystemConnectors()
            .remove(CAMUNDA_8_SELF_MANAGED_DEFAULT_REST_ADDRESS);

    if (camunda8OutboundSystemConnector != null) {
      adapterManager
          .getOutboundSystemConnectors()
          .put(camunda8RestAddress, camunda8OutboundSystemConnector);
    }
  }
}
