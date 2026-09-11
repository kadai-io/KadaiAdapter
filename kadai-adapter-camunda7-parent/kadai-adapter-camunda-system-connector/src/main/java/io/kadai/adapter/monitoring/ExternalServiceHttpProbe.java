package io.kadai.adapter.monitoring;

import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class ExternalServiceHttpProbe {

  private final RestClient restClient;

  ExternalServiceHttpProbe(RestClient restClient) {
    this.restClient = restClient;
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

    try {
      T body = response.bodyTo(bodyType);
      if (body == null) {
        return HttpProbeResult.invalidResponse(statusCode, "Response body is empty");
      }
      return HttpProbeResult.response(statusCode, body);
    } catch (RuntimeException e) {
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
