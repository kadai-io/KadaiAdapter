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
            .withDetail("Outbox Service", response.getBody())
            .withDetail("baseUrl", urlString)
            .build();
      } else {
        return Health.down()
            .withDetail("Outbox Service Error", "Unexpected status: " + response.getStatusCode())
            .withDetail("baseUrl", urlString)
            .build();
      }

    } catch (Exception e) {
      return Health.down()
          .withDetail("Outbox Service Error", e.getMessage())
          .withDetail("baseUrl", urlString)
          .build();
    }
  }

  private ResponseEntity<OutboxEventCountRepresentationModel> pingOutBoxRest() {
    return restTemplate.getForEntity(url, OutboxEventCountRepresentationModel.class);
  }
}
