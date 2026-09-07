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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class Camunda7HealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";
  private static final String ENGINE_PATH_SEGMENT = "engine";

  private final ExternalServiceHttpProbe httpProbe;
  private final HttpHeaderProvider httpHeaderProvider;
  private final URI url;
  private final String expectedEngineName;

  Camunda7HealthIndicator(
      ExternalServiceHttpProbe httpProbe,
      HttpHeaderProvider httpHeaderProvider,
      Camunda7System camunda7System) {
    this.httpProbe = httpProbe;
    this.httpHeaderProvider = httpHeaderProvider;
    this.url = createEngineListUrl(camunda7System.getSystemRestUrl());
    this.expectedEngineName = determineExpectedEngineName(camunda7System);
  }

  @Override
  public Health health() {
    HttpHeaders headers;
    try {
      headers = httpHeaderProvider.camunda7RestApiHeaders();
    } catch (RuntimeException e) {
      return downForFailure("client-error", "Unable to create authentication headers", null)
          .build();
    }
    HttpProbeResult<Camunda7EngineInfoRepresentationModel[]> result =
        httpProbe.getJson(url, headers, Camunda7EngineInfoRepresentationModel[].class);

    if (result.failureType() != HttpProbeResult.FailureType.NONE) {
      return downForFailure(
          healthFailureType(result.failureType()),
          result.failureMessage(),
          httpStatus(result))
          .build();
    }

    if (!result.isHttp200()) {
      return downForFailure(
          "http-status",
          "Unexpected HTTP status: " + result.statusCode(),
          httpStatus(result))
          .build();
    }

    Camunda7EngineInfoRepresentationModel[] engines = result.body();
    if (engines == null || engines.length == 0) {
      return downForFailure("semantic-mismatch", "No engines found", 200).build();
    }

    Camunda7EngineInfoRepresentationModel expectedEngine =
        expectedEngineName == null ? null : findEngine(engines, expectedEngineName);
    if (expectedEngine == null && expectedEngineName != null) {
      return downForFailure(
              "semantic-mismatch",
              "Expected engine '" + expectedEngineName + "' not found",
              200)
          .withDetail("camundaEngines", engines)
          .build();
    }
    if (expectedEngine != null) {
      return Health.up()
          .withDetail("camundaEngine", expectedEngine)
          .withDetail(BASE_URL, url)
          .build();
    }
    return Health.up().withDetail("camundaEngines", engines).withDetail(BASE_URL, url).build();
  }

  private Health.Builder downForFailure(
      String failureType, String error, Integer httpStatus) {
    Health.Builder builder =
        Health.down()
            .withDetail("camundaEngineError", errorOrFailureType(error, failureType))
            .withDetail("failureType", failureType)
            .withDetail(BASE_URL, url);
    if (httpStatus != null) {
      builder.withDetail("httpStatus", httpStatus);
    }
    return builder;
  }

  private static String healthFailureType(HttpProbeResult.FailureType failureType) {
    return switch (failureType) {
      case INVALID_RESPONSE -> "invalid-response";
      case TRANSPORT_ERROR -> "transport-error";
      case CLIENT_ERROR -> "client-error";
      case NONE -> "client-error";
    };
  }

  private static Integer httpStatus(HttpProbeResult<?> result) {
    return result.statusCode() == null ? null : result.statusCode().value();
  }

  private static String errorOrFailureType(String error, String failureType) {
    return error == null || error.isBlank() ? "Health probe failed: " + failureType : error;
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

  private static Camunda7EngineInfoRepresentationModel findEngine(
      Camunda7EngineInfoRepresentationModel[] engines, String expectedEngineName) {
    return Arrays.stream(engines)
        .filter(engine -> engine != null)
        .filter(engine -> expectedEngineName.equals(engine.getName()))
        .findFirst()
        .orElse(null);
  }
}
