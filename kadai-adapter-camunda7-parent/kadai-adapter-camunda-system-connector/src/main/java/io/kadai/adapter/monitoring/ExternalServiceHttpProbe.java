package io.kadai.adapter.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class ExternalServiceHttpProbe {

  private final RestClient restClient;
  private final JsonMapper jsonMapper;

  ExternalServiceHttpProbe(RestClient restClient, JsonMapper jsonMapper) {
    this.restClient = restClient;
    this.jsonMapper = jsonMapper;
  }

  <T> HttpProbeResult<T> getJson(URI uri, HttpHeaders headers, Class<T> bodyType) {
    try {
      return restClient
          .get()
          .uri(uri)
          .headers(requestHeaders -> requestHeaders.addAll(headers))
          .exchange((request, response) -> readResponse(response, bodyType));
    } catch (ResourceAccessException e) {
      return HttpProbeResult.transportError(safeMessage(e));
    } catch (RestClientException e) {
      return HttpProbeResult.clientError(safeMessage(e));
    } catch (RuntimeException e) {
      return HttpProbeResult.clientError(safeMessage(e));
    }
  }

  private <T> HttpProbeResult<T> readResponse(
      RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
      Class<T> bodyType) {
    HttpStatusCode statusCode;
    try {
      statusCode = response.getStatusCode();
    } catch (IOException e) {
      return HttpProbeResult.transportError(safeMessage(e));
    }

    if (statusCode.value() != 200) {
      return HttpProbeResult.response(statusCode, null);
    }

    byte[] responseBody;
    try {
      InputStream body = response.getBody();
      if (body == null) {
        return HttpProbeResult.invalidResponse(statusCode, "Response body is empty");
      }
      try (body) {
        responseBody = body.readAllBytes();
      }
    } catch (IOException e) {
      return HttpProbeResult.transportError(safeMessage(e));
    }

    if (responseBody.length == 0) {
      return HttpProbeResult.invalidResponse(statusCode, "Response body is empty");
    }

    try {
      T body =
          jsonMapper
              .readerFor(bodyType)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readValue(responseBody);
      return HttpProbeResult.response(statusCode, body);
    } catch (Exception e) {
      return HttpProbeResult.invalidResponse(
          statusCode, "Response body could not be decoded as JSON");
    }
  }

  private static String safeMessage(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getSimpleName();
    }
    return message;
  }
}
