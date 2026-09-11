package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxDataSource;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import java.util.Map;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
class KadaiAdapterHealthTest extends AbsIntegrationTest {

  @Test
  void should_ReturnUp_When_AllContributorsAreUp() {
    ResponseEntity<Map> response =
        restClient.get().uri("/actuator/health/kadaiAdapter").retrieve().toEntity(Map.class);
    Map<String, Object> body = response.getBody();

    assertThat(body).isNotNull();
    assertThat(body).extracting("status").isEqualTo("UP");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator/health/kadaiAdapter/plugin/camunda7",
        "/actuator/health/kadaiAdapter/plugin/camunda7/default",
        "/actuator/health/kadaiAdapter/plugin/camunda7/default/camunda",
        "/actuator/health/kadaiAdapter/plugin/camunda7/default/outbox"
      })
  void should_ReturnUp_ForCamunda7HealthContributors(String uri) {
    ResponseEntity<Map> response = restClient.get().uri(uri).retrieve().toEntity(Map.class);
    Map<String, Object> body = response.getBody();

    assertThat(body).isNotNull();
    assertThat(body).extracting("status").isEqualTo("UP");
  }

  @Test
  void should_ReportOutboxDown_When_OutboxPersistenceFails() {
    ResponseEntity<Map> successfulCountResponse =
        restClient
            .get()
            .uri("/outbox-rest/events/count?retries=0")
            .retrieve()
            .toEntity(Map.class);
    assertThat(successfulCountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(successfulCountResponse.getBody()).containsKey("eventsCount");

    PooledDataSource outboxDataSource = (PooledDataSource) OutboxDataSource.get();
    String originalUrl = outboxDataSource.getUrl();
    outboxDataSource.setUrl("jdbc:invalid:outbox-health-test");
    try {
      ResponseEntity<String> failedCountResponse =
          restClient
              .get()
              .uri("/outbox-rest/events/count?retries=0")
              .retrieve()
              .onStatus(statusCode -> statusCode.isError(), (request, response) -> {})
              .toEntity(String.class);

      assertThat(failedCountResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(failedCountResponse.getBody()).isEqualTo("Outbox persistence is unavailable");

      ResponseEntity<Map> healthResponse =
          restClient
              .get()
              .uri("/actuator/health/kadaiAdapter/plugin/camunda7/default/outbox")
              .retrieve()
              .onStatus(statusCode -> statusCode.isError(), (request, response) -> {})
              .toEntity(Map.class);

      assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      Map<String, Object> healthBody = healthResponse.getBody();
      assertThat(healthBody).isNotNull().containsEntry("status", "DOWN");
      Map<String, Object> details = (Map<String, Object>) healthBody.get("details");
      assertThat(details)
          .containsEntry("failureType", "http-status")
          .containsEntry("httpStatus", 503);
      assertThat(details.get("outboxServiceError").toString())
          .doesNotContainIgnoringCase("event_store");
    } finally {
      outboxDataSource.setUrl(originalUrl);
    }
  }
}
