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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@TestPropertySource(properties = {"kadai.adapter.xsrf.token=KAD_UNIQUE_TOKEN_123"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class TestHttpHeaderProviderWithCsrfToken {
  @Autowired HttpHeaderProvider httpHeaderProvider;

  @Test
  void should_ReturnHttpHeaderWithCsrfToken_When_PropertyExists() {
    HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
    assertThat(headers).containsKeys("Authorization", "Cookie", "X-XSRF-TOKEN");
    assertThat(headers.get("Cookie")).containsExactly("XSRF-TOKEN=KAD_UNIQUE_TOKEN_123");
    assertThat(headers.get("X-XSRF-TOKEN")).containsExactly("KAD_UNIQUE_TOKEN_123");
  }
}
