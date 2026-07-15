package io.kadai.adapter.systemconnector.camunda.acceptance;

import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext
@TestPropertySource(
    properties = {
      "camunda.client.worker.defaults.stream-enabled=true",
      "camunda.client.worker.defaults.poll-interval=PT1M"
    })
@KadaiAdapterCamunda8SpringBootTest
class JobWorkerStreamingAccTest extends AbstractJobWorkerCommunicationAccTest {

  @Override
  protected boolean expectedPreferRestOverGrpc() {
    return true;
  }

  @Override
  protected boolean expectedStreamEnabled() {
    return true;
  }
}
