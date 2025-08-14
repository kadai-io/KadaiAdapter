package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.models.OutboxEventCountRepresentationModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class OutboxHealthIndicatorTest {

  private OutboxHealthIndicator outboxHealthIndicator;
  private RestClient restClient;
  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() throws IOException {
    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();
    this.restClient = RestClient.builder().build();
    String baseUrl = mockWebServer.url("/outbox-rest").toString();
    this.outboxHealthIndicator = new OutboxHealthIndicator(restClient, baseUrl);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void should_ReturnUp_When_OutboxRespondsSuccessfully() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String body = mapper.writeValueAsString(new OutboxEventCountRepresentationModel());

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .setBody(body));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void should_ReturnDown_When_OutboxRespondsWithError(HttpStatus httpStatus) {
    mockWebServer.enqueue(new MockResponse().setResponseCode(httpStatus.value()));

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void should_ReturnDown_When_OutboxPingFails() {
    try {
      mockWebServer.shutdown();
    } catch (IOException e) {
      // ignore
    }

    assertThat(outboxHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  private static Stream<Arguments> errorResponseProvider() {
    return Arrays.stream(HttpStatus.values()).filter(HttpStatus::isError).map(Arguments::of);
  }
}
