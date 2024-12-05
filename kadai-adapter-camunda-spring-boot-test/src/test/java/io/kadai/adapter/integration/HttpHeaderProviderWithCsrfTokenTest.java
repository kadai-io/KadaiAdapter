package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.test.KadaiAdapterTestApplicationWithoutCamunda;
import io.kadai.common.test.security.JaasExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@SpringBootTest(
    classes = {KadaiAdapterTestApplicationWithoutCamunda.class},
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "kadai.adapter.xsrf.token=KAD_UNIQUE_TOKEN_123",
      "camunda.bpm.enabled=false",
    })
@ExtendWith(JaasExtension.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class HttpHeaderProviderWithCsrfTokenTest {

  @Autowired private HttpHeaderProvider httpHeaderProvider;

  @Test
  void should_ReturnHttpHeaderWithCsrfToken_When_PropertyExists() {
    HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
    assertThat(headers).containsKeys("Authorization", "Cookie", "X-XSRF-TOKEN");
    assertThat(headers.get("Cookie")).containsExactly("XSRF-TOKEN=KAD_UNIQUE_TOKEN_123");
    assertThat(headers.get("X-XSRF-TOKEN")).containsExactly("KAD_UNIQUE_TOKEN_123");
  }
}
