package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class OutboxHealthIndicatorTest {

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
    OutboxHealthIndicator outboxHealthIndicator = new OutboxHealthIndicator(restClient, BASE_URL);
    OutboxEventCountRepresentationModel outboxEventCount =
        new OutboxEventCountRepresentationModel();

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(OutboxEventCountRepresentationModel.class))
        .thenReturn(ResponseEntity.ok(outboxEventCount));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_OutboxRespondsWithError(HttpStatus httpStatus) {
    OutboxHealthIndicator outboxHealthIndicator = new OutboxHealthIndicator(restClient, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(OutboxEventCountRepresentationModel.class))
        .thenThrow(new RuntimeException("HTTP " + httpStatus.value()));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_OutboxPingFails() {
    OutboxHealthIndicator outboxHealthIndicator = new OutboxHealthIndicator(restClient, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI)))
        .thenThrow(new RuntimeException("Connection failed"));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
