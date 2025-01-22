package io.kadai.adapter.integration;

import static io.kadai.adapter.integration.HealthCheckEndpoints.CAMUNDA_ENGINE_ENDPOINT;
import static io.kadai.adapter.integration.HealthCheckEndpoints.HEALTH_ENDPOINT;
import static io.kadai.adapter.integration.HealthCheckEndpoints.OUTBOX_CSRF_ENDPOINT;
import static io.kadai.adapter.integration.HealthCheckEndpoints.OUTBOX_EVENTS_COUNT_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.kadai.adapter.camunda.outbox.rest.filter.CsrfTokenService;
import io.kadai.adapter.monitoring.CamundaHealthCheck;
import io.kadai.adapter.monitoring.SchedulerHealthCheck;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CamundaHealthCheckTest extends AbsIntegrationTest {

  @Autowired private SchedulerHealthCheck schedulerHealthIndicator;

  private MockRestServiceServer mockServer;

  @Autowired private RestTemplate restTemplate;

  @Autowired private CamundaHealthCheck camundaHealthIndicator;

  @Autowired private TestRestTemplate testRestTemplate;

  @Autowired private CsrfTokenService csrfTokenService;

  @BeforeEach
  @WithAccessId(user = "admin")
  void setUp() throws Exception {
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

    SchedulerHealthCheck spySchedulerHealthCheck = schedulerHealthIndicator;
    Instant validRunTime = Instant.now().minus(Duration.ofMinutes(5));
    when(spySchedulerHealthCheck.health())
        .thenReturn(Health.up().withDetail("Last Run", validRunTime).build());
    dummyCsrfMock();
    dummyOutboxMock();
  }

  @AfterEach
  void tearDown() {
    mockServer.reset();
  }

  @AfterAll
  void triggerSetUp() {
    isInitialised = false;
  }

  @Test
  void should_ReturnUp_When_CamundaServiceIsHealthy() {
    mockServer
        .expect(ExpectedCount.manyTimes(), requestTo(CAMUNDA_ENGINE_ENDPOINT))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[{\"name\": \"default\"}]", MediaType.APPLICATION_JSON));

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("\"Camunda Health\":{\"status\":\"UP\"")
        .contains("\"Camunda Engines\":[{\"name\":\"default\"}]");
  }

  @Test
  void should_ReturnDown_When_CheckingCamundaEngineHealth() {
    mockServer
        .expect(ExpectedCount.manyTimes(), requestTo(CAMUNDA_ENGINE_ENDPOINT))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody())
        .contains("\"Camunda Health\":{\"status\":\"DOWN\"")
        .contains("\"Camunda Engine Error\":\"No engines found\"");
  }

  @Test
  void should_ThrowException_When_CheckingCamundaEngineHealth() {
    mockServer
        .expect(ExpectedCount.manyTimes(), requestTo(CAMUNDA_ENGINE_ENDPOINT))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.NOT_FOUND)
                .body("Page Not Found")
                .contentType(MediaType.TEXT_PLAIN));

    ResponseEntity<String> response = testRestTemplate.getForEntity(HEALTH_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody())
        .contains("\"Camunda Health\":{\"status\":\"DOWN\"")
        .contains("\"Camunda Engine Error\":");
  }

  private void dummyOutboxMock() {
    this.mockServer
        .expect(ExpectedCount.manyTimes(), requestTo(OUTBOX_EVENTS_COUNT_ENDPOINT))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"eventsCount\": 5}", MediaType.APPLICATION_JSON));
  }

  private void dummyCsrfMock() {
    String randomToken = csrfTokenService.createRandomToken();
    HttpHeaders responseHeaders = new HttpHeaders();
    String tokenCookie = "XSRF-TOKEN=" + randomToken + "; Path=/; HttpOnly";
    responseHeaders.add(HttpHeaders.SET_COOKIE, tokenCookie);
    mockServer
        .expect(ExpectedCount.manyTimes(), requestTo(OUTBOX_CSRF_ENDPOINT))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NO_CONTENT).headers(responseHeaders));
  }
}
