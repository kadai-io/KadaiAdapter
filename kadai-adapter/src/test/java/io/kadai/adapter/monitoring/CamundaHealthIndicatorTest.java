package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.models.CamundaEngineInfoRepresentationModel;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class CamundaHealthIndicatorTest {

  private CamundaHealthIndicator camundaHealthIndicatorSpy;
  private RestClient restClient;

  @BeforeEach
  void setUp() {
    this.restClient = Mockito.mock(RestClient.class);
    this.camundaHealthIndicatorSpy =
        Mockito.spy(new CamundaHealthIndicator(restClient, "http://localhost:10020/engine-rest"));
  }

  @Test
  void should_ReturnUp_When_CamundaRespondsSuccessfully() {
    CamundaEngineInfoRepresentationModel[] engines =
        new CamundaEngineInfoRepresentationModel[] {new CamundaEngineInfoRepresentationModel()};
    ResponseEntity<CamundaEngineInfoRepresentationModel[]> response = ResponseEntity.ok(engines);

    Mockito.doReturn(response).when(camundaHealthIndicatorSpy).pingCamundaRest();

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_CamundaRespondsSuccessfullyButListsNoEngines() {
    ResponseEntity<CamundaEngineInfoRepresentationModel[]> response =
        ResponseEntity.ok(new CamundaEngineInfoRepresentationModel[] {});

    Mockito.doReturn(response).when(camundaHealthIndicatorSpy).pingCamundaRest();

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_CamundaRespondsWithError(HttpStatus httpStatus) {
    ResponseEntity<CamundaEngineInfoRepresentationModel[]> response =
        ResponseEntity.status(httpStatus).build();

    Mockito.doReturn(response).when(camundaHealthIndicatorSpy).pingCamundaRest();

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_CamundaPingFails() {
    Mockito.doThrow(new RuntimeException("foo")).when(camundaHealthIndicatorSpy).pingCamundaRest();

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
