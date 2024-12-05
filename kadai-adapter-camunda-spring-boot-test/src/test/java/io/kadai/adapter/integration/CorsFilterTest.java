package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class CorsFilterTest {
  @Autowired private TestRestTemplate testRestTemplate;

  @Test
  void should_ReturnCorsHeaders_When_CallingGetEventsEndpoint() {

    String url = "http://localhost:10020/outbox-rest/events";

    ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders responseHeaders = response.getHeaders();
    assertThat(responseHeaders.containsKey("Access-Control-Allow-Origin")).isTrue();
    assertThat(responseHeaders.containsKey("Access-Control-Allow-Credentials")).isTrue();
    assertThat(responseHeaders.containsKey("Access-Control-Allow-Headers")).isTrue();
    assertThat(responseHeaders.containsKey("Access-Control-Allow-Methods")).isTrue();

    assertThat(responseHeaders.get("Access-Control-Allow-Origin").get(0)).isEqualTo("*");
    assertThat(responseHeaders.get("Access-Control-Allow-Credentials").get(0)).isEqualTo("true");
    assertThat(responseHeaders.get("Access-Control-Allow-Headers").get(0))
        .isEqualTo("origin, content-type, accept, authorization");
    assertThat(responseHeaders.get("Access-Control-Allow-Methods").get(0))
        .isEqualTo("GET, POST, PUT, DELETE, OPTIONS, HEAD");
  }
}
