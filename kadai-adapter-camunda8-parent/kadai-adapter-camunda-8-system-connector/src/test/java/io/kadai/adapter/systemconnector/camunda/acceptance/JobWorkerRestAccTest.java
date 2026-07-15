package io.kadai.adapter.systemconnector.camunda.acceptance;

import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
@TestPropertySource(properties = "camunda.client.prefer-rest-over-grpc=true")
@KadaiAdapterCamunda8SpringBootTest
class JobWorkerRestAccTest extends AbstractJobWorkerCommunicationAccTest {

  @Override
  protected boolean expectedPreferRestOverGrpc() {
    return true;
  }
}
