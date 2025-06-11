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
  private URI url;

  public CamundaHealthIndicator(RestTemplate restTemplate, String urlString) {
    this.restTemplate = restTemplate;
    this.url =
        UriComponentsBuilder.fromUriString(urlString)
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
        return Health.down().withDetail("camundaEngineError", "No engines found").build();
      }
      return Health.up().withDetail("camundaEngines", engines).build();
    } catch (Exception e) {
      return Health.down().withDetail("camundaEngines", e.getMessage()).build();
    }
  }

  private ResponseEntity<CamundaEngineInfoRepresentationModel[]> pingCamundaRest() {
    return restTemplate.getForEntity(url, CamundaEngineInfoRepresentationModel[].class);
  }
}
