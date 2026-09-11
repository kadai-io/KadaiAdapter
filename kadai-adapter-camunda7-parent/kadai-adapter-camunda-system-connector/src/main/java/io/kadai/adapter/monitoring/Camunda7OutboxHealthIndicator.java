package io.kadai.adapter.monitoring;

import io.kadai.adapter.monitoring.models.OutboxEventCountRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.net.URI;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class Camunda7OutboxHealthIndicator implements HealthIndicator {

  private static final String BASE_URL = "baseUrl";

  private final ExternalServiceHttpProbe httpProbe;
  private final HttpHeaderProvider httpHeaderProvider;
  private final URI url;
  private final String urlString;

  public Camunda7OutboxHealthIndicator(
      RestClient restClient, HttpHeaderProvider httpHeaderProvider, String urlString) {
    this.httpProbe = new ExternalServiceHttpProbe(restClient);
    this.httpHeaderProvider = httpHeaderProvider;
    this.url =
        UriComponentsBuilder.fromUriString(urlString)
            .pathSegment("events")
            .pathSegment("count")
            .queryParam("retries", 0)
            .build()
            .toUri();
    this.urlString = urlString;
  }

  @Override
  public Health health() {
    HttpHeaders headers;
    try {
      headers = httpHeaderProvider.outboxRestApiHeaders();
    } catch (RuntimeException e) {
      return downForFailure("client-error", "Unable to create authentication headers", null)
          .build();
    }
    HttpProbeResult<OutboxEventCountProbeResponse> result =
        httpProbe.getJson(url, headers, OutboxEventCountProbeResponse.class);

    if (result.failureType() != HttpProbeResult.FailureType.NONE) {
      return downForFailure(
          HealthProbeFailureSupport.healthFailureType(result.failureType()),
          result.failureMessage(),
          HealthProbeFailureSupport.httpStatus(result))
          .build();
    }

    if (!result.isHttp200()) {
      return downForFailure(
          "http-status",
          "Unexpected HTTP status: " + result.statusCode(),
          HealthProbeFailureSupport.httpStatus(result))
          .build();
    }

    OutboxEventCountProbeResponse body = result.body();
    if (body == null || body.getEventsCount() == null || body.getEventsCount() < 0) {
      return downForFailure(
              "semantic-mismatch", "Invalid Outbox event-count response", 200)
          .build();
    }

    OutboxEventCountRepresentationModel details = new OutboxEventCountRepresentationModel();
    details.setEventsCount(body.getEventsCount());
    return Health.up()
        .withDetail("outboxService", details)
        .withDetail(BASE_URL, urlString)
        .build();
  }

  private Health.Builder downForFailure(
      String failureType, String error, Integer httpStatus) {
    Health.Builder builder =
        Health.down()
            .withDetail(
                "outboxServiceError",
                HealthProbeFailureSupport.errorOrFailureType(error, failureType))
            .withDetail("failureType", failureType)
            .withDetail(BASE_URL, urlString);
    if (httpStatus != null) {
      builder.withDetail("httpStatus", httpStatus);
    }
    return builder;
  }

}
