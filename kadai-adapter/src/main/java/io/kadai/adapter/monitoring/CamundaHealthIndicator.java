package io.kadai.adapter.monitoring;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class CamundaHealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";

  private final RestTemplate restTemplate;
  private URI url;
  private String urlString;

  public CamundaHealthIndicator(RestTemplate restTemplate, String urlString) {
    this.restTemplate = restTemplate;
    this.urlString = urlString;
    this.url = UriComponentsBuilder.fromUriString(urlString).pathSegment("engine").build().toUri();
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<CamundaEngineInfoRepresentationModel[]> response = pingCamundaRest();
      CamundaEngineInfoRepresentationModel[] engines = response.getBody();

      if (engines == null || engines.length == 0) {
        return Health.down()
            .withDetail("camundaEngineError", "No engines found")
            .withDetail(BASE_URL, urlString)
            .build();
      }
      return Health.up().withDetail("camundaEngines", engines).withDetail(BASE_URL, url).build();
    } catch (Exception e) {
      return Health.down()
          .withDetail("camundaEngines", e.getMessage())
          .withDetail(BASE_URL, urlString)
          .build();
    }
  }

  private ResponseEntity<CamundaEngineInfoRepresentationModel[]> pingCamundaRest() {
    return restTemplate.getForEntity(url, CamundaEngineInfoRepresentationModel[].class);
  }
}
