package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class OutboxHealthCheck implements HealthIndicator {

  private final RestTemplate restTemplate;
  private final String outboxAddress;
  private final Integer outboxPort;
  private final String contextPath;
  private final String outboxEndpointPath;
  private final String outboxQuery;
  private URI url;

  public OutboxHealthCheck(
      RestTemplate restTemplate,
      String outboxAddress,
      Integer outboxPort,
      String contextPath,
      String outboxEndpointPath,
      String outboxQuery) {
    this.restTemplate = restTemplate;
    this.outboxAddress = outboxAddress;
    this.outboxPort = outboxPort;
    this.contextPath = contextPath;
    this.outboxEndpointPath = outboxEndpointPath;
    this.outboxQuery = outboxQuery;
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
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(outboxAddress);

    if (outboxPort != null) {
      if (outboxPort <= 0) {
        throw new IllegalArgumentException(
            "Port must be a positive integer. Provided: " + outboxPort);
      }
      builder.port(outboxPort);
    }

    if (contextPath != null) {
      builder.pathSegment(contextPath).pathSegment(outboxEndpointPath);
    }

    if (outboxQuery != null) {
      builder.query(outboxQuery);
    }

    this.url = builder.build().toUri();
  }

  private ResponseEntity<OutboxEventCountRepresentationModel> pingOutBoxRest() {
    return restTemplate.getForEntity(url, OutboxEventCountRepresentationModel.class);
  }
}
