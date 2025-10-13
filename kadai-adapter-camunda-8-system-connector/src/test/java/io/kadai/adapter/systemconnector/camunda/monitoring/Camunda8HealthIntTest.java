package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.tasklistener.KadaiAdapterCamunda8SpringBootTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

@KadaiAdapterCamunda8SpringBootTest
@SuppressWarnings({"unchecked", "rawtypes"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class Camunda8HealthIntTest {

  @LocalServerPort private Integer port;

  @Test
  void should_ReturnUnknownOnInitialState() {
    RestClient restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .build();

    ResponseEntity<Map> response =
        restClient
            .get()
            .uri("/actuator/health/kadaiAdapter/plugin/camunda8")
            .retrieve()
            .toEntity(Map.class);
    Map<String, Object> body = response.getBody();

    assertThat(body).isNotNull();
    assertThat(body).extracting("status").isEqualTo("UNKNOWN");
  }
}
