package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
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
class CamundaHealthIndicatorTest {

  private static final String BASE_URL = "http://localhost:8080/engine-rest";
  private static final URI EXPECTED_URI =
      UriComponentsBuilder.fromUriString(BASE_URL).pathSegment("engine").build().toUri();

  @Mock RestClient restClient;

  @Test
  void should_ReturnUp_When_CamundaRespondsSuccessfully() {
    CamundaHealthIndicator camundaHealthIndicator =
        new CamundaHealthIndicator(restClient, BASE_URL);
    CamundaEngineInfoRepresentationModel engine = new CamundaEngineInfoRepresentationModel();
    CamundaEngineInfoRepresentationModel[] engines = {engine};

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(CamundaEngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(engines));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_CamundaRespondsSuccessfullyButListsNoEngines() {
    CamundaHealthIndicator camundaHealthIndicator =
        new CamundaHealthIndicator(restClient, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(CamundaEngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(new CamundaEngineInfoRepresentationModel[0]));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_CamundaRespondsWithError(HttpStatus httpStatus) {
    CamundaHealthIndicator camundaHealthIndicator =
        new CamundaHealthIndicator(restClient, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(CamundaEngineInfoRepresentationModel[].class))
        .thenThrow(new RuntimeException("HTTP " + httpStatus.value()));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_CamundaPingFails() {
    CamundaHealthIndicator camundaHealthIndicator =
        new CamundaHealthIndicator(restClient, BASE_URL);

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(eq(EXPECTED_URI)))
        .thenThrow(new RuntimeException("Connection failed"));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
