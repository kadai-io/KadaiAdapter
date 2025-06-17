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
  private final String camundaOutboxAddress;
  private final int camundaOutboxPort;
  private final String contextPath;
  private URI url;

  public OutboxHealthIndicator(
      RestTemplate restTemplate,
      String camundaOutboxAddress,
      int camundaOutboxPort,
      String contextPath) {
    this.restTemplate = restTemplate;
    this.camundaOutboxAddress = camundaOutboxAddress;
    this.camundaOutboxPort = camundaOutboxPort;
    this.contextPath = contextPath;
    init();
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

  private void init() {
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
  }

  private ResponseEntity<OutboxEventCountRepresentationModel> pingOutBoxRest() {
    return restTemplate.getForEntity(url, OutboxEventCountRepresentationModel.class);
  }
}
