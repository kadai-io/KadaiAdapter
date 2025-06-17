package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class CamundaHealthIndicator implements HealthIndicator {

  private final RestTemplate restTemplate;
  private final String camundaOutboxAddress;
  private final int camundaOutboxPort;
  private final String contextPath;

  private URI url;

  public CamundaHealthIndicator(
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
      ResponseEntity<CamundaEngineInfoRepresentationModel[]> response = pingCamundaRest();
      CamundaEngineInfoRepresentationModel[] engines = response.getBody();

      if (engines == null || engines.length == 0) {
        return Health.down().withDetail("camundaEngineError", "No engines found").build();
      }
      return Health.up().withDetail("camundaEngines", engines).build();
    } catch (Exception e) {
      return Health.down().withDetail("camundaEngines", e.getMessage()).build();
    }
  }

  private void init() {
    this.url =
        UriComponentsBuilder.fromUriString(camundaOutboxAddress)
            .port(camundaOutboxPort)
            .pathSegment(contextPath)
            .pathSegment("engine-rest")
            .pathSegment("engine")
            .build()
            .toUri();
  }

  private ResponseEntity<CamundaEngineInfoRepresentationModel[]> pingCamundaRest() {
    return restTemplate.getForEntity(url, CamundaEngineInfoRepresentationModel[].class);
  }
}
