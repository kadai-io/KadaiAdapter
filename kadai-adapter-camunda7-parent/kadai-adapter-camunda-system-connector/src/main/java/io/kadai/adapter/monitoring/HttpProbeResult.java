package io.kadai.adapter.monitoring;

import org.springframework.http.HttpStatusCode;

record HttpProbeResult<T>(
    HttpStatusCode statusCode,
    T body,
    HttpProbeResult.FailureType failureType,
    String failureMessage) {

  enum FailureType {
    NONE,
    INVALID_RESPONSE,
    TRANSPORT_ERROR,
    CLIENT_ERROR
  }

  static <T> HttpProbeResult<T> response(HttpStatusCode statusCode, T body) {
    return new HttpProbeResult<>(statusCode, body, FailureType.NONE, null);
  }

  static <T> HttpProbeResult<T> invalidResponse(
      HttpStatusCode statusCode, String failureMessage) {
    return new HttpProbeResult<>(
        statusCode, null, FailureType.INVALID_RESPONSE, failureMessage);
  }

  static <T> HttpProbeResult<T> transportError(String failureMessage) {
    return new HttpProbeResult<>(null, null, FailureType.TRANSPORT_ERROR, failureMessage);
  }

  static <T> HttpProbeResult<T> clientError(String failureMessage) {
    return new HttpProbeResult<>(null, null, FailureType.CLIENT_ERROR, failureMessage);
  }

  boolean isHttp200() {
    return statusCode != null && statusCode.value() == 200;
  }
}
