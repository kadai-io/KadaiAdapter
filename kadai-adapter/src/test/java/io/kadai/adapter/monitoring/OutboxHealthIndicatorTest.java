package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
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

class OutboxHealthIndicatorTest {

  private OutboxHealthIndicator outboxHealthIndicatorSpy;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    this.restTemplate = Mockito.mock(RestTemplate.class);
    this.outboxHealthIndicatorSpy =
        Mockito.spy(
            new OutboxHealthIndicator(
                restTemplate, "http://localhost", 8090, "example-context-root"));
  }

  @Test
  void should_ReturnUp_When_OutboxRespondsSuccessfully() {
    when(restTemplate.<OutboxEventCountRepresentationModel>getForEntity(any(), any()))
        .thenReturn(ResponseEntity.ok().body(new OutboxEventCountRepresentationModel()));

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_OutboxRespondsWithError(HttpStatus httpStatus) {
    when(restTemplate.<OutboxEventCountRepresentationModel>getForEntity(any(), any()))
        .thenReturn(ResponseEntity.status(httpStatus).build());

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_OutboxPingFails() {
    when(restTemplate.<OutboxEventCountRepresentationModel>getForEntity(any(), any()))
        .thenThrow(new RuntimeException("foo"));

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
