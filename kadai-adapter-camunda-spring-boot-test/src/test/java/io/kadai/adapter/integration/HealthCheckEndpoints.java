package io.kadai.adapter.integration;

public class HealthCheckEndpoints {
  public static final String BASE_URL = "http://localhost:10020";
  public static final String HEALTH_ENDPOINT = BASE_URL + "/actuator/health/external-services";
  public static final String CAMUNDA_ENGINE_ENDPOINT = BASE_URL + "/engine-rest/engine";
  public static final String OUTBOX_EVENTS_COUNT_ENDPOINT =
      BASE_URL + "/outbox-rest/events/count?retries=0";
  public static final String OUTBOX_CSRF_ENDPOINT =
      BASE_URL + "/outbox-rest/events/csrf";

  private HealthCheckEndpoints() {}
}
