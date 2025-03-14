package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest
class HttpHeaderProviderXsrfTest {
  @Nested
  @SpringBootTest
  class WithoutXsrfTokenTest {
    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Test
    void should_ReturnHttpHeaderWithoutXsrfToken_When_PropertyNotExists() {
      HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
      assertThat(headers).containsOnlyKeys("Authorization");
    }
  }

  @Nested
  @SpringBootTest
  @TestPropertySource(properties = {"kadai.adapter.xsrf.token=KAD_UNIQUE_TOKEN_123"})
  class WithXsrfTokenTest {
    @Autowired private HttpHeaderProvider httpHeaderProvider;

    @Test
    void should_ReturnHttpHeaderWithXsrfToken_When_PropertyExists() {
      HttpHeaders headers = httpHeaderProvider.outboxRestApiHeaders();
      assertThat(headers).containsKeys("Authorization", "Cookie", "X-XSRF-TOKEN");
      assertThat(headers.get("Cookie")).containsExactly("XSRF-TOKEN=KAD_UNIQUE_TOKEN_123");
      assertThat(headers.get("X-XSRF-TOKEN")).containsExactly("KAD_UNIQUE_TOKEN_123");
    }
  }
}
