package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    return restTemplate.getForEntity(url, OutboxEventCountRepresentationModel.class);
  }
}
