package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.web.client.RestClient;

class OutboxHealthIndicatorTest {

  private OutboxHealthIndicator outboxHealthIndicatorSpy;
  private RestClient restClient;

  @BeforeEach
  void setUp() {
    this.restClient = Mockito.mock(RestClient.class);
    this.outboxHealthIndicatorSpy =
        Mockito.spy(new OutboxHealthIndicator(restClient, "http://localhost:10020/outbox-rest"));
  }

  @Test
  void should_ReturnUp_When_OutboxRespondsSuccessfully() {
    ResponseEntity<OutboxEventCountRepresentationModel> response =
        ResponseEntity.ok(new OutboxEventCountRepresentationModel());

    Mockito.doReturn(response).when(outboxHealthIndicatorSpy).pingOutBoxRest();

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.UP);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_OutboxRespondsWithError(HttpStatus httpStatus) {
    ResponseEntity<OutboxEventCountRepresentationModel> response =
        ResponseEntity.status(httpStatus).build();

    Mockito.doReturn(response).when(outboxHealthIndicatorSpy).pingOutBoxRest();

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_OutboxPingFails() {
    Mockito.doThrow(new RuntimeException("foo")).when(outboxHealthIndicatorSpy).pingOutBoxRest();

    assertThat(outboxHealthIndicatorSpy.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
