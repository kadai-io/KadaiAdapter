package io.kadai.adapter.systemconnector.camunda.acceptance;

import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
@TestPropertySource(properties = "camunda.client.prefer-rest-over-grpc=false")
@KadaiAdapterCamunda8SpringBootTest
class JobWorkerGrpcAccTest extends AbstractJobWorkerCommunicationAccTest {

  @Override
  protected boolean expectedPreferRestOverGrpc() {
    return false;
  }
}
