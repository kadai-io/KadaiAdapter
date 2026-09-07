package io.kadai.adapter.monitoring;

final class HealthProbeFailureSupport {

  static String healthFailureType(HttpProbeResult.FailureType failureType) {
    return switch (failureType) {
      case INVALID_RESPONSE -> "invalid-response";
      case TRANSPORT_ERROR -> "transport-error";
      case CLIENT_ERROR -> "client-error";
      case NONE -> throw new IllegalArgumentException("NONE is not a failure");
    };
  }

  static Integer httpStatus(HttpProbeResult<?> result) {
    return result.statusCode() == null ? null : result.statusCode().value();
  }

  static String errorOrFailureType(String error, String failureType) {
    return error == null || error.isBlank() ? "Health probe failed: " + failureType : error;
  }

  private HealthProbeFailureSupport() {}
}
