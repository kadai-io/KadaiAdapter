package io.kadai.adapter.monitoring;

import io.kadai.adapter.monitoring.models.Camunda7EngineInfoRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class Camunda7HealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";
  private static final String ENGINE_PATH_SEGMENT = "engine";

  private final RestClient restClient;
  private final HttpHeaderProvider httpHeaderProvider;
  private final Camunda7System camunda7System;
  private final URI url;
  private final String expectedEngineName;

  public Camunda7HealthIndicator(
      RestClient restClient, HttpHeaderProvider httpHeaderProvider, Camunda7System camunda7System) {
    this.restClient = restClient;
    this.httpHeaderProvider = httpHeaderProvider;
    this.camunda7System = camunda7System;
    this.url = createEngineListUrl(camunda7System.getSystemRestUrl());
    this.expectedEngineName = determineExpectedEngineName(camunda7System);
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<Camunda7EngineInfoRepresentationModel[]> response = pingCamunda7Rest();
      Camunda7EngineInfoRepresentationModel[] engines = response.getBody();

      if (engines == null || engines.length == 0) {
        return Health.down()
            .withDetail("camundaEngineError", "No engines found")
            .withDetail(BASE_URL, camunda7System.getSystemRestUrl())
            .build();
      }
      if (expectedEngineName != null && !containsEngine(engines, expectedEngineName)) {
        return Health.down()
            .withDetail(
                "camundaEngineError", "Expected engine '" + expectedEngineName + "' not found")
            .withDetail("camundaEngines", engines)
            .withDetail(BASE_URL, url)
            .build();
      }
      return Health.up().withDetail("camundaEngines", engines).withDetail(BASE_URL, url).build();
    } catch (Exception e) {
      return Health.down()
          .withDetail("camundaEngines", e.getMessage())
          .withDetail(BASE_URL, camunda7System.getSystemRestUrl())
          .build();
    }
  }

  ResponseEntity<Camunda7EngineInfoRepresentationModel[]> pingCamunda7Rest() {
    HttpHeaders headers = httpHeaderProvider.camunda7RestApiHeaders();
    return restClient
        .get()
        .uri(url)
        .headers(h -> h.addAll(headers))
        .retrieve()
        .toEntity(Camunda7EngineInfoRepresentationModel[].class);
  }

  private static URI createEngineListUrl(String systemRestUrl) {
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(systemRestUrl).build();
    List<String> pathSegments = uriComponents.getPathSegments();
    int engineSegmentIndex = pathSegments.lastIndexOf(ENGINE_PATH_SEGMENT);

    if (engineSegmentIndex >= 0 && engineSegmentIndex < pathSegments.size() - 1) {
      String engineListPath =
          "/" + String.join("/", pathSegments.subList(0, engineSegmentIndex + 1));
      return UriComponentsBuilder.fromUri(uriComponents.toUri())
          .replacePath(engineListPath)
          .replaceQuery(null)
          .fragment(null)
          .build()
          .toUri();
    }

    return UriComponentsBuilder.fromUriString(systemRestUrl)
        .pathSegment(ENGINE_PATH_SEGMENT)
        .build()
        .toUri();
  }

  private static String determineExpectedEngineName(Camunda7System camunda7System) {
    if (camunda7System.getCamunda7EngineIdentifier() != null
        && !camunda7System.getCamunda7EngineIdentifier().isBlank()) {
      return camunda7System.getCamunda7EngineIdentifier();
    }

    List<String> pathSegments =
        UriComponentsBuilder.fromUriString(camunda7System.getSystemRestUrl())
            .build()
            .getPathSegments();
    int engineSegmentIndex = pathSegments.lastIndexOf(ENGINE_PATH_SEGMENT);

    if (engineSegmentIndex >= 0 && engineSegmentIndex < pathSegments.size() - 1) {
      return pathSegments.get(engineSegmentIndex + 1);
    }
    return null;
  }

  private static boolean containsEngine(
      Camunda7EngineInfoRepresentationModel[] engines, String expectedEngineName) {
    for (Camunda7EngineInfoRepresentationModel engine : engines) {
      if (Objects.equals(expectedEngineName, engine.getName())) {
        return true;
      }
    }
    return false;
  }
}
