package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "kadai-adapter.plugin.camunda7.systems[0].system-rest-url="
          + "${camunda7.testcontainers.rest-url}/engine/default",
      "kadai-adapter.plugin.camunda7.systems[0].system-task-event-url="
          + "${camunda7.testcontainers.outbox-url}",
      "kadai-adapter.plugin.camunda7.systems[0].camunda7-engine-identifier=default"
    })
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
class Camunda7EngineScopedHealthIntegrationTest {

  private static final String CAMUNDA_SYSTEM_REST_URL_PROPERTY =
      "kadai-adapter.plugin.camunda7.systems[0].system-rest-url";

  @LocalServerPort private Integer port;

  @Value("${kadai-adapter.plugin.camunda7.systems[0].system-rest-url}")
  private String camundaSystemRestUrl;

  private RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .build();
  }

  @Test
  void should_UseEngineListEndpoint_When_CamundaSystemRestUrlIsEngineScoped() {
    ResponseEntity<Map> response =
        restClient
            .get()
            .uri("/actuator/health/kadaiAdapter/plugin/camunda7/default/camunda")
            .retrieve()
            .toEntity(Map.class);
    Map<String, Object> body = response.getBody();

    assertThat(camundaSystemRestUrl)
        .isEqualTo(System.getProperty(CAMUNDA_SYSTEM_REST_URL_PROPERTY) + "/engine/default");
    assertThat(body).isNotNull();
    assertThat(body).extracting("status").isEqualTo("UP");
    assertThat((Map<String, Object>) body.get("details"))
        .containsEntry("baseUrl", System.getProperty(CAMUNDA_SYSTEM_REST_URL_PROPERTY) + "/engine");
  }
}
