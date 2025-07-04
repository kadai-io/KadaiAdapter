package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.springframework.web.client.RestTemplate;

class CamundaHealthIndicatorTest {

  private CamundaHealthIndicator camundaHealthIndicatorSpy;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    this.restTemplate = Mockito.mock(RestTemplate.class);
    this.camundaHealthIndicatorSpy =
        Mockito.spy(
            new CamundaHealthIndicator(
                restTemplate, "http://localhost:10020/engine-rest"));
  }

  @Test
  void should_ReturnUp_When_CamundaRespondsSuccessfully() {
    when(restTemplate.<CamundaEngineInfoRepresentationModel[]>getForEntity(any(), any()))
        .thenReturn(
            ResponseEntity.ok()
                .body(
                    new CamundaEngineInfoRepresentationModel[] {
                      new CamundaEngineInfoRepresentationModel()
                    }));

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void should_ReturnDown_When_CamundaRespondsSuccessfullyButListsNoEngines() {
    when(restTemplate.<CamundaEngineInfoRepresentationModel[]>getForEntity(any(), any()))
        .thenReturn(ResponseEntity.ok().body(new CamundaEngineInfoRepresentationModel[] {}));

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_CamundaRespondsWithError(HttpStatus httpStatus) {
    when(restTemplate.<CamundaEngineInfoRepresentationModel[]>getForEntity(any(), any()))
        .thenReturn(ResponseEntity.status(httpStatus).build());

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_CamundaPingFails() {
    when(restTemplate.<CamundaEngineInfoRepresentationModel[]>getForEntity(any(), any()))
        .thenThrow(new RuntimeException("foo"));

    assertThat(camundaHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
