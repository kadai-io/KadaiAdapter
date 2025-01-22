package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.filter.CsrfTokenService;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResource;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.internal.util.Pair;
import io.kadai.common.internal.util.Triplet;
import io.kadai.common.test.security.JaasExtension;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class CsrfTokenRetrieverTest {

  @LocalServerPort private Integer port;
  @Autowired private HttpHeaderProvider httpHeaderProvider;
  @Autowired private CsrfTokenService csrfTokenService;

  private final String csrfTokenCookieAndHeaderNotMatch =
      "CSRF token cookie and header does not match";
  private final String csrfTokenCookieInvalid = "CSRF token is invalid.";
  private final String csrfTokenHeaderNull = "Missing X-XSRF-TOKEN Header.";
  private final String csrfTokenHeaderWrongSize = "There should only be one X-XSRF-TOKEN Header";
  private final String xsrfTokenCookieNull = "Missing XSRF-TOKEN Cookie";
  private final String xsrfTokenCookieWrongSize = "There should only be one XSRF-TOKEN Cookie";

  @Test
  void should_InitializeValidCsrfTokenInHttpHeaderProvider() {
    assertThat(csrfTokenService.validateToken(httpHeaderProvider.getCsrfToken().replace("\"", "")))
        .isTrue();
  }

  @Test
  void should_ValidateToken_When_TokenIsValid() {
    String token = csrfTokenService.createRandomToken();
    assertThat(csrfTokenService.validateToken(token)).isTrue();
  }

  @TestFactory
  Stream<DynamicTest> should_NotValidateToken_When_TokenIsInvalid() {
    byte[] data = new byte[48];
    new SecureRandom().nextBytes(data);

    List<Pair<String, String>> testCases =
        List.of(
            Pair.of("Not a Base64 String", "AAABAgMEBQYHCAk@DA0ODwAhERITFBUWFXd4IUA=="),
            Pair.of("Not in Proper Format", "invalidToken"),
            Pair.of("Wrong Signature", Base64.getEncoder().encodeToString(data)));
    ThrowingConsumer<Pair<String, String>> test =
        t -> {
          assertThat(csrfTokenService.validateToken(t.getRight())).isFalse();
        };
    return DynamicTest.stream(testCases.iterator(), Pair::getLeft, test);
  }

  @Test
  void should_RetrieveCsrfToken_When_CallingCsrfEndpoint() {
    TestRestTemplate restTemplate =
        new TestRestTemplate(
            new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(HttpComponentsClientHttpRequestFactory.class));

    String url = "http://localhost:10020/outbox-rest/events/csrf";
    ResponseEntity<CamundaTaskEventListResource> answer =
        restTemplate.exchange(url, HttpMethod.GET, null, CamundaTaskEventListResource.class);

    assertThat(answer.getHeaders()).isNotNull();
    assertThat(answer.getHeaders().containsKey("Set-Cookie"));
    String xsrfTokenCookie =
        answer.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
            .filter(cookie -> cookie.contains("XSRF-TOKEN="))
            .findFirst()
            .map(
                cookie -> {
                  int startIndex = cookie.indexOf("XSRF-TOKEN=") + "XSRF-TOKEN=".length();
                  int endIndex = cookie.indexOf(';', startIndex);
                  return endIndex == -1
                      ? cookie.substring(startIndex)
                      : cookie.substring(startIndex, endIndex);
                })
            .orElse(null);
    assertThat(xsrfTokenCookie).isNotNull();
    assertThat(csrfTokenService.validateToken(xsrfTokenCookie.replace("\"", ""))).isTrue();
  }

  @Test
  void should_NotRetrieveCsrfToken_When_CallingCsrfEndpointAndValidCookieAlreadyExists() {
    TestRestTemplate restTemplate =
        new TestRestTemplate(
            new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(HttpComponentsClientHttpRequestFactory.class));
    String url = "http://localhost:10020/outbox-rest/events/csrf";

    HttpEntity<Void> requestEntity = httpHeaderProvider.prepareNewEntityForOutboxRestApi();
    ResponseEntity<String> answer =
        restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

    assertThat(answer.getHeaders().get("Set-Cookie")).isNull();
  }

  @TestFactory
  Stream<DynamicTest> should_NotValidateRequests_When_HeaderNotContainValidCookieHeaders() {

    HttpHeaders headers1 = new HttpHeaders();
    headers1.add("X-XSRF-TOKEN", "Token");
    HttpHeaders headers2 = new HttpHeaders();
    headers2.add("Cookie", "XSRF-TOKEN=Token1; XSRF-TOKEN=Token2");
    headers2.add("X-XSRF-TOKEN", "Token");
    HttpHeaders headers3 = new HttpHeaders();
    headers3.add("Cookie", "XSRF-TOKEN=" + "Token");
    HttpHeaders headers4 = new HttpHeaders();
    headers4.add("Cookie", "XSRF-TOKEN=" + "Token1");
    headers4.add("X-XSRF-TOKEN", "Token2");
    headers4.add("X-XSRF-TOKEN", "Token3");
    HttpHeaders headers5 = new HttpHeaders();
    headers5.add("Cookie", "XSRF-TOKEN=" + "Token1");
    headers5.add("X-XSRF-TOKEN", "Token2");
    HttpHeaders headers6 = new HttpHeaders();
    headers6.add("Cookie", "XSRF-TOKEN=" + "invalidToken");
    headers6.add("X-XSRF-TOKEN", "invalidToken");

    String url = "http://localhost:10020/outbox-rest/events";
    List<Triplet<String, HttpHeaders, String>> testCases =
        List.of(
            Triplet.of("No XSRF-TOKEN Cookie", headers1, xsrfTokenCookieNull),
            Triplet.of("Multiple XSRF-TOKEN Cookies", headers2, xsrfTokenCookieWrongSize),
            Triplet.of("No X-XSRF-TOKEN Header", headers3, csrfTokenHeaderNull),
            Triplet.of("Multiple X-XSRF-TOKEN Headers", headers4, csrfTokenHeaderWrongSize),
            Triplet.of(
                "Cookie and XSRF Header Token Not Equal",
                headers5,
                csrfTokenCookieAndHeaderNotMatch),
            Triplet.of("Invalid Token", headers6, csrfTokenCookieInvalid));
    ThrowingConsumer<Triplet<String, HttpHeaders, String>> test =
        t -> {
          TestRestTemplate restTemplate =
              new TestRestTemplate(
                  new RestTemplateBuilder()
                      .rootUri("http://localhost:" + port)
                      .requestFactory(HttpComponentsClientHttpRequestFactory.class));
          HttpEntity<Void> requestEntity = new HttpEntity<>(t.getMiddle());
          ResponseEntity<String> answer =
              restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

          assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(answer.getBody()).isEqualTo(t.getRight());
        };

    return DynamicTest.stream(testCases.iterator(), Triplet::getLeft, test);
  }

  @Test
  void should_GetEvents_When_HeaderContainsValidCookieAndToken() {
    TestRestTemplate restTemplate =
        new TestRestTemplate(
            new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(HttpComponentsClientHttpRequestFactory.class));
    String url = "http://localhost:10020/outbox-rest/events";

    HttpEntity<Void> requestEntity = httpHeaderProvider.prepareNewEntityForOutboxRestApi();
    ResponseEntity<CamundaTaskEventListResource> answer =
        restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, CamundaTaskEventListResource.class);
    assertThat(answer.getBody()).isNotNull();
  }
}
