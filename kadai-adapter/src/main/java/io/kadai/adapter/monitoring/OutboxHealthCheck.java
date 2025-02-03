package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OutboxHealthCheck implements HealthIndicator {

  private final RestTemplate restTemplate;

  @Value("${camundaOutboxService.address:http://localhost}")
  private String camundaOutboxAddress;

  @Value("${camundaOutboxService.port:8090}")
  private int camundaOutboxPort;

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  private URI url;

  private URI csrfUrl;

  public OutboxHealthCheck(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostConstruct
  public void init() {
    this.url =
        UriComponentsBuilder.fromUriString(camundaOutboxAddress)
            .port(camundaOutboxPort)
            .pathSegment(contextPath)
            .pathSegment("outbox-rest")
            .pathSegment("events")
            .pathSegment("count")
            .queryParam("retries", 0)
            .build()
            .toUri();

    this.csrfUrl =
        UriComponentsBuilder.fromUriString(camundaOutboxAddress)
            .port(camundaOutboxPort)
            .pathSegment(contextPath)
            .pathSegment("outbox-rest")
            .pathSegment("events")
            .pathSegment("csrf")
            .build()
            .toUri();
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<OutboxEventCountRepresentationModel> response = pingOutBoxRest();

      if (response.getStatusCode() == HttpStatus.OK) {
        return Health.up().withDetail("Outbox Service", response.getBody()).build();
      } else {
        return Health.down()
            .withDetail("Outbox Service Error", "Unexpected status: " + response.getStatusCode())
            .build();
      }

    } catch (Exception e) {
      return Health.down().withDetail("Outbox Service Error", e.getMessage()).build();
    }
  }

  private ResponseEntity<OutboxEventCountRepresentationModel> pingOutBoxRest() throws Exception {
    replaceCookieStore(restTemplate);
    ResponseEntity<String> responseEntity =
        restTemplate.exchange(csrfUrl, HttpMethod.GET, null, String.class);
    HttpHeaders responseHeaders = responseEntity.getHeaders();
    List<String> cookies = responseHeaders.get(HttpHeaders.SET_COOKIE);

    if (cookies == null || cookies.isEmpty()) {
      throw new Exception("No cookies found in the response.");
    }

    String xsrfToken = null;

    for (String cookie : cookies) {
      if (cookie.startsWith("XSRF-TOKEN=")) {
        xsrfToken = cookie.split(";")[0].substring("XSRF-TOKEN=".length());
      }
    }

    if (xsrfToken == null) {
      throw new Exception("XSRF-TOKEN cookie not found in the response.");
    }
    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "XSRF-TOKEN=" + xsrfToken);
    headers.add("X-XSRF-TOKEN", xsrfToken);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    return restTemplate.exchange(
        url, HttpMethod.GET, requestEntity, OutboxEventCountRepresentationModel.class);
  }

  private void replaceCookieStore(RestTemplate restTemplate) {
    if (restTemplate.getRequestFactory()
        instanceof HttpComponentsClientHttpRequestFactory factory) {
      HttpClient httpClient =
          HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();

      factory.setHttpClient(httpClient);
    }
  }
}
