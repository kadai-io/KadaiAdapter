package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.models.OutboxEventCountRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class Camunda7OutboxHealthIndicatorTest {

  private static final String BASE_URL = "http://localhost:8080/outbox-rest";
  private static final URI EXPECTED_URI =
      UriComponentsBuilder.fromUriString(BASE_URL)
          .pathSegment("events")
          .pathSegment("count")
          .queryParam("retries", 0)
          .build()
          .toUri();

  @Mock RestClient restClient;

  @Test
  void should_ReturnUp_When_OutboxRespondsSuccessfully() {
    Camunda7OutboxHealthIndicator outboxHealthIndicator =
        new Camunda7OutboxHealthIndicator(restClient, mockHttpHeaderProvider(), BASE_URL);
    OutboxEventCountRepresentationModel outboxEventCount =
        new OutboxEventCountRepresentationModel();

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(OutboxEventCountRepresentationModel.class))
        .thenReturn(ResponseEntity.ok(outboxEventCount));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_OutboxRespondsWithError(HttpStatus httpStatus) {
    Camunda7OutboxHealthIndicator outboxHealthIndicator =
        new Camunda7OutboxHealthIndicator(restClient, mockHttpHeaderProvider(), BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(OutboxEventCountRepresentationModel.class))
        .thenThrow(new RuntimeException("HTTP " + httpStatus.value()));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_OutboxPingFails() {
    Camunda7OutboxHealthIndicator outboxHealthIndicator =
        new Camunda7OutboxHealthIndicator(restClient, mockHttpHeaderProvider(), BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenThrow(new RuntimeException("Connection failed"));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_SendAuthenticationHeaders_When_PingingOutbox() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.add("Authorization", "Basic dXNlcjpwYXNz");
    when(httpHeaderProvider.outboxRestApiHeaders()).thenReturn(authHeaders);

    Camunda7OutboxHealthIndicator outboxHealthIndicator =
        new Camunda7OutboxHealthIndicator(restClient, httpHeaderProvider, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(OutboxEventCountRepresentationModel.class))
        .thenReturn(ResponseEntity.ok(new OutboxEventCountRepresentationModel()));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  private HttpHeaderProvider mockHttpHeaderProvider() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    when(httpHeaderProvider.outboxRestApiHeaders()).thenReturn(new HttpHeaders());
    return httpHeaderProvider;
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
