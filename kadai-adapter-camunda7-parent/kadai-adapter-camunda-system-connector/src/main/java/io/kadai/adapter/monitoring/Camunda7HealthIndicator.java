package io.kadai.adapter.monitoring;

import io.kadai.adapter.monitoring.models.Camunda7EngineInfoRepresentationModel;
import java.net.URI;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class Camunda7HealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";

  private final RestClient restClient;
  private URI url;
  private String urlString;

  public Camunda7HealthIndicator(RestClient restClient, String urlString) {
    this.restClient = restClient;
    this.urlString = urlString;
    this.url = UriComponentsBuilder.fromUriString(urlString).pathSegment("engine").build().toUri();
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<Camunda7EngineInfoRepresentationModel[]> response = pingCamunda7Rest();
      Camunda7EngineInfoRepresentationModel[] engines = response.getBody();

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

  ResponseEntity<Camunda7EngineInfoRepresentationModel[]> pingCamunda7Rest() {
    return restClient
        .get()
        .uri(url)
        .retrieve()
        .toEntity(Camunda7EngineInfoRepresentationModel[].class);
  }
}
