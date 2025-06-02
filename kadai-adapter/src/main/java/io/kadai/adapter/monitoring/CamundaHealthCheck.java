package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class CamundaHealthCheck implements HealthIndicator {

  private final RestTemplate restTemplate;
  private final String camundaAddress;
  private final Integer camundaPort;
  private final String contextPath;
  private final String camundaEndpointPath;
  private final String camundaQuery;

  private URI url;

  public CamundaHealthCheck(
      RestTemplate restTemplate,
      String camundaAddress,
      Integer camundaPort,
      String contextPath,
      String camundaEndpointPath,
      String camundaQuery) {
    this.restTemplate = restTemplate;
    this.camundaAddress = camundaAddress;
    this.camundaPort = camundaPort;
    this.contextPath = contextPath;
    this.camundaEndpointPath = camundaEndpointPath;
    this.camundaQuery = camundaQuery;
    init();
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

  private void init() {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(camundaAddress);

    if (camundaPort != null) {
      if (camundaPort <= 0) {
        throw new IllegalArgumentException(
            "Port must be a positive integer. Provided: " + camundaPort);
      }
      builder.port(camundaPort);
    }

    if (contextPath != null) {
      builder.pathSegment(contextPath).pathSegment(camundaEndpointPath);
    }

    if (camundaQuery != null) {
      builder.query(camundaQuery);
    }

    this.url = builder.build().toUri();
  }

  private ResponseEntity<CamundaEngineInfoRepresentationModel[]> pingCamundaRest() {
    return restTemplate.getForEntity(url, CamundaEngineInfoRepresentationModel[].class);
  }
}
