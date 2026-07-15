package io.kadai.adapter.systemconnector.camunda.acceptance;

import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@KadaiAdapterCamunda8SpringBootTest
class JobWorkerDefaultRestAccTest extends AbstractJobWorkerCommunicationAccTest {

  @Override
  protected boolean expectedPreferRestOverGrpc() {
    return true;
  }
}
