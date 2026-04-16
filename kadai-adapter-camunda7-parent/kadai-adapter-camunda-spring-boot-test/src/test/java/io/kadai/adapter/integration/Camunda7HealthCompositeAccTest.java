package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
class Camunda7HealthCompositeAccTest {

  @LocalServerPort private Integer port;

  private RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .build();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator/health/kadaiAdapter/plugin/camunda7",
        "/actuator/health/kadaiAdapter/plugin/camunda7/camundaSystem1",
        "/actuator/health/kadaiAdapter/plugin/camunda7/camundaSystem1/camunda",
        "/actuator/health/kadaiAdapter/plugin/camunda7/camundaSystem1/outbox"
      })
  void should_ReturnUp_For(String uri) {
    ResponseEntity<Map> response = restClient.get().uri(uri).retrieve().toEntity(Map.class);
    Map<String, Object> body = response.getBody();

    assertThat(body).isNotNull();
    assertThat(body).extracting("status").isEqualTo("UP");
  }
}
