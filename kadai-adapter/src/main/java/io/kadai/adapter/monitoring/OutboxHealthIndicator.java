package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class OutboxHealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";

  private final RestTemplate restTemplate;
  private URI url;
  private String urlString;

  public OutboxHealthIndicator(RestTemplate restTemplate, String urlString) {
    this.restTemplate = restTemplate;
    this.url =
        UriComponentsBuilder.fromUriString(urlString)
            .pathSegment("events")
            .pathSegment("count")
            .queryParam("retries", 0)
            .build()
            .toUri();
    this.urlString = urlString;
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<OutboxEventCountRepresentationModel> response = pingOutBoxRest();

      if (response.getStatusCode() == HttpStatus.OK) {
        return Health.up()
            .withDetail("outboxService", response.getBody())
            .withDetail(BASE_URL, urlString)
            .build();
      } else {
        return Health.down()
            .withDetail("outboxServiceError", "Unexpected status: " + response.getStatusCode())
            .withDetail(BASE_URL, urlString)
            .build();
      }

    } catch (Exception e) {
      return Health.down()
          .withDetail("outboxServiceError", e.getMessage())
          .withDetail(BASE_URL, urlString)
          .build();
    }
  }

  private ResponseEntity<OutboxEventCountRepresentationModel> pingOutBoxRest() {
    return restTemplate.getForEntity(url, OutboxEventCountRepresentationModel.class);
  }
}
