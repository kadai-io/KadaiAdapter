package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestHttpHeaderProviderWithoutCsrf extends AbsIntegrationTest {

  @Autowired private HttpHeaderProvider httpHeaderProvider;

  @Test
  void should_ReturnHttpHeaderWithoutCsrfToken_When_PropertyNotExists() {
    HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
    assertThat(headers).containsOnlyKeys("Authorization");
  }
}
