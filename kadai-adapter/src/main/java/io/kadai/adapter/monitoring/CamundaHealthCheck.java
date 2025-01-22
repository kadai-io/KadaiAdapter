package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CamundaHealthCheck implements HealthIndicator {

  private final RestTemplate restTemplate;

  @Value("${camundaOutboxService.address:http://localhost}")
  private String camundaOutboxAddress;

  @Value("${camundaOutboxService.port:8090}")
  private int camundaOutboxPort;

  @Value("${outbox.context-path:}")
  private String contextPath;

  private URI url;

  public CamundaHealthCheck(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostConstruct
  public void init() {
    this.url =
        UriComponentsBuilder.fromUriString(camundaOutboxAddress)
            .port(camundaOutboxPort)
            .pathSegment(contextPath)
            .pathSegment("engine-rest")
            .pathSegment("engine")
            .build()
            .toUri();
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<CamundaEngineInfoRepresentationModel[]> response = pingCamundaRest();
      CamundaEngineInfoRepresentationModel[] engines = response.getBody();

      if (engines == null || engines.length == 0) {
        return Health.down().withDetail("Camunda Engine Error", "No engines found").build();
      }
      return Health.up().withDetail("Camunda Engines", engines).build();
    } catch (Exception e) {
      return Health.down().withDetail("Camunda Engine Error", e.getMessage()).build();
    }
  }

  private ResponseEntity<CamundaEngineInfoRepresentationModel[]> pingCamundaRest()
      throws Exception {

    return restTemplate.getForEntity(url, CamundaEngineInfoRepresentationModel[].class);
  }
}
