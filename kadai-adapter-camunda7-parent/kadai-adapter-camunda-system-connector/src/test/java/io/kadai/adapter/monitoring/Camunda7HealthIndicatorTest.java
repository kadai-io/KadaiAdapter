package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.models.Camunda7EngineInfoRepresentationModel;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.adapter.systemconnector.camunda.config.Camunda7System;
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
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class Camunda7HealthIndicatorTest {

  private static final String BASE_URL = "http://localhost:8080/engine-rest";
  private static final URI EXPECTED_URI =
      UriComponentsBuilder.fromUriString(BASE_URL).pathSegment("engine").build().toUri();
  private static final String ENGINE_SCOPED_BASE_URL =
      "http://localhost:8080/rest/engine/default";
  private static final URI EXPECTED_ENGINE_SCOPED_URI =
      UriComponentsBuilder.fromUriString("http://localhost:8080/rest/engine").build().toUri();

  @Mock RestClient restClient;

  @Test
  void should_ReturnUp_When_CamundaRespondsSuccessfully() {
    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(restClient, mockHttpHeaderProvider(), camunda7System(BASE_URL));
    Camunda7EngineInfoRepresentationModel engine = new Camunda7EngineInfoRepresentationModel();
    Camunda7EngineInfoRepresentationModel[] engines = {engine};

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(engines));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnUp_When_CamundaRespondsSuccessfullyForEngineScopedUrl() {
    Camunda7EngineInfoRepresentationModel engine = new Camunda7EngineInfoRepresentationModel();
    engine.setName("default");
    Camunda7EngineInfoRepresentationModel[] engines = {engine};

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_ENGINE_SCOPED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(engines));

    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(
            restClient,
            mockHttpHeaderProvider(),
            camunda7System(ENGINE_SCOPED_BASE_URL, "default"));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_ConfiguredEngineIsNotListed() {
    Camunda7EngineInfoRepresentationModel engine = new Camunda7EngineInfoRepresentationModel();
    engine.setName("other-engine");
    Camunda7EngineInfoRepresentationModel[] engines = {engine};

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_ENGINE_SCOPED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(engines));

    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(
            restClient,
            mockHttpHeaderProvider(),
            camunda7System(ENGINE_SCOPED_BASE_URL, "default"));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_CamundaRespondsSuccessfullyButListsNoEngines() {
    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(restClient, mockHttpHeaderProvider(), camunda7System(BASE_URL));

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(new Camunda7EngineInfoRepresentationModel[0]));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_CamundaRespondsWithError(HttpStatus httpStatus) {
    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(restClient, mockHttpHeaderProvider(), camunda7System(BASE_URL));

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenThrow(new RuntimeException("HTTP " + httpStatus.value()));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_CamundaPingFails() {
    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(restClient, mockHttpHeaderProvider(), camunda7System(BASE_URL));

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenThrow(new RuntimeException("Connection failed"));

    Health health = camundaHealthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("camundaEngineError", "Connection failed")
        .containsEntry("baseUrl", EXPECTED_URI);
  }

  @Test
  void should_SendAuthenticationHeaders_When_PingingCamunda() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.add("Authorization", "Basic dXNlcjpwYXNz");
    when(httpHeaderProvider.camunda7RestApiHeaders()).thenReturn(authHeaders);

    Camunda7HealthIndicator camundaHealthIndicator =
        new Camunda7HealthIndicator(restClient, httpHeaderProvider, camunda7System(BASE_URL));

    RestClient.RequestHeadersUriSpec mockRequestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);
    Camunda7EngineInfoRepresentationModel[] engines = {new Camunda7EngineInfoRepresentationModel()};

    when(restClient.get()).thenReturn(mockRequestSpec);
    when(mockRequestSpec.uri(EXPECTED_URI)).thenReturn(mockRequestSpec);
    when(mockRequestSpec.headers(any())).thenReturn(mockRequestSpec);
    when(mockRequestSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.toEntity(Camunda7EngineInfoRepresentationModel[].class))
        .thenReturn(ResponseEntity.ok(engines));

    assertThat(camundaHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  private HttpHeaderProvider mockHttpHeaderProvider() {
    HttpHeaderProvider httpHeaderProvider = mock(HttpHeaderProvider.class);
    when(httpHeaderProvider.camunda7RestApiHeaders()).thenReturn(new HttpHeaders());
    return httpHeaderProvider;
  }

  private Camunda7System camunda7System(String systemRestUrl) {
    Camunda7System camunda7System = new Camunda7System();
    camunda7System.setSystemRestUrl(systemRestUrl);
    return camunda7System;
  }

  private Camunda7System camunda7System(String systemRestUrl, String engineIdentifier) {
    Camunda7System camunda7System = camunda7System(systemRestUrl);
    camunda7System.setCamunda7EngineIdentifier(engineIdentifier);
    return camunda7System;
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
