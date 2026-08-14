package io.kadai.adapter.monitoring;

import io.kadai.adapter.monitoring.models.Camunda7EngineInfoRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
  private final URI url;
  private final String expectedEngineName;

  public Camunda7HealthIndicator(
      RestClient restClient, HttpHeaderProvider httpHeaderProvider, Camunda7System camunda7System) {
    this.restClient = restClient;
    this.httpHeaderProvider = httpHeaderProvider;
    this.url = createEngineListUrl(camunda7System.getSystemRestUrl());
    this.expectedEngineName = determineExpectedEngineName(camunda7System);
  }

  @Override
  public Health health() {
    try {
      ResponseEntity<Camunda7EngineInfoRepresentationModel[]> response = pingCamunda7Rest();
      Camunda7EngineInfoRepresentationModel[] engines = response.getBody();

      if (engines == null || engines.length == 0) {
        return down("No engines found", null);
      }
      if (expectedEngineName != null && !containsEngine(engines, expectedEngineName)) {
        return down("Expected engine '" + expectedEngineName + "' not found", engines);
      }
      return Health.up().withDetail("camundaEngines", engines).withDetail(BASE_URL, url).build();
    } catch (Exception e) {
      return down(e.getMessage(), null);
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

  /**
   * Creates the Camunda engine-list endpoint from the configured system REST URL.
   *
   * <p>For an engine-scoped URL, the trailing engine name is removed. A URL already ending in
   * {@code engine} is preserved; otherwise, {@code engine} is appended.
   *
   * @param systemRestUrl configured Camunda REST URL
   * @return the endpoint that lists the available Camunda engines
   */
  private static URI createEngineListUrl(String systemRestUrl) {
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(systemRestUrl).build();
    List<String> pathSegments = uriComponents.getPathSegments();
    int engineSegmentIndex = findEngineSegmentIndex(pathSegments);

    if (engineSegmentIndex >= 0) {
      String engineListPath =
          "/" + String.join("/", pathSegments.subList(0, engineSegmentIndex + 1));
      return UriComponentsBuilder.fromUri(uriComponents.toUri())
          .replacePath(engineListPath)
          .replaceQuery(null)
          .fragment(null)
          .build()
          .toUri();
    }

    if (pathSegments.contains(ENGINE_PATH_SEGMENT)) {
      return UriComponentsBuilder.fromUri(uriComponents.toUri())
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

  /**
   * Determines the Camunda engine expected from this system's configuration.
   *
   * <p>An explicit engine identifier takes precedence over an engine name present in the system
   * REST URL.
   *
   * @param camunda7System Camunda system configuration
   * @return the expected engine name, or {@code null} when none is configured or implied
   */
  private static String determineExpectedEngineName(Camunda7System camunda7System) {
    if (camunda7System.getCamunda7EngineIdentifier() != null
        && !camunda7System.getCamunda7EngineIdentifier().isBlank()) {
      return camunda7System.getCamunda7EngineIdentifier();
    }

    List<String> pathSegments =
        UriComponentsBuilder.fromUriString(camunda7System.getSystemRestUrl())
            .build()
            .getPathSegments();
    int engineSegmentIndex = findEngineSegmentIndex(pathSegments);

    if (engineSegmentIndex >= 0) {
      return pathSegments.get(engineSegmentIndex + 1);
    }
    return null;
  }

  private static int findEngineSegmentIndex(List<String> pathSegments) {
    int index = pathSegments.lastIndexOf(ENGINE_PATH_SEGMENT);
    return index >= 0 && index < pathSegments.size() - 1 ? index : -1;
  }

  private static boolean containsEngine(
      Camunda7EngineInfoRepresentationModel[] engines, String expectedEngineName) {
    return Arrays.stream(engines)
        .map(Camunda7EngineInfoRepresentationModel::getName)
        .anyMatch(expectedEngineName::equals);
  }

  private Health down(String error, Object engines) {
    Health.Builder builder = Health.down().withDetail("camundaEngineError", error);
    if (engines != null) {
      builder.withDetail("camundaEngines", engines);
    }
    return builder.withDetail(BASE_URL, this.url).build();
  }
}
