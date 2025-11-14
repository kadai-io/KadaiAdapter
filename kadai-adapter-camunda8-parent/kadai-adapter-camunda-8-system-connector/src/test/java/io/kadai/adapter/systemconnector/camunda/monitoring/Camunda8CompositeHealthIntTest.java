package io.kadai.adapter.systemconnector.camunda.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.camunda.client.CamundaClient;
import io.kadai.adapter.systemconnector.camunda.Camunda8TestUtil;
import io.kadai.adapter.systemconnector.camunda.KadaiAdapterCamunda8SpringBootTest;
import io.kadai.adapter.test.KadaiAdapterTestUtil;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.test.security.WithAccessId;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@KadaiAdapterCamunda8SpringBootTest
@SuppressWarnings({"unchecked", "rawtypes"})
class Camunda8CompositeHealthIntTest {

  @LocalServerPort private Integer port;
  @Autowired private Camunda8TestUtil camunda8TestUtil;
  @Autowired private CamundaClient client;
  @Autowired private KadaiAdapterTestUtil kadaiAdapterTestUtil;
  @Autowired private KadaiEngine kadaiEngine;

  @Nested
  @DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
  class Camunda8CompositeUp {
    @Test
    @WithAccessId(user = "admin")
    void should_ReturnUp_When_AnyJobWorkerRanSuccessfullyAndAllOthersHaveNotAtAll()
        throws Exception {
      kadaiAdapterTestUtil.createWorkbasket("GPK_KSC", "DOMAIN_A");
      kadaiAdapterTestUtil.createClassification("L11010", "DOMAIN_A");
      client
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .send()
          .join();

      client.newCreateInstanceCommand().bpmnProcessId("Test_Process").latestVersion().send().join();

      camunda8TestUtil.waitUntil(
          () -> !kadaiEngine.getTaskService().createTaskQuery().list().isEmpty());

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
      assertThat(body).extracting("status").isEqualTo("UP");
    }
  }

  @Nested
  @DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
  class Camunda8CompositeUnknown {
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

  @Nested
  @DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
  class Camunda8CompositeDown {
    @Test
    @WithAccessId(user = "admin")
    void should_ReturnDown_When_AnyJobWorkerRanWithError() {
      client
          .newDeployResourceCommand()
          .addResourceFromClasspath("processes/sayHello.bpmn")
          .send()
          .join();

      // workbasket doesn't exist => failure
      client.newCreateInstanceCommand().bpmnProcessId("Test_Process").latestVersion().send().join();

      RestClient restClient =
          RestClient.builder()
              .baseUrl("http://localhost:" + port)
              .requestFactory(new HttpComponentsClientHttpRequestFactory())
              .build();

      final ThrowingCallable call =
          () ->
              restClient
                  .get()
                  .uri("/actuator/health/kadaiAdapter/plugin/camunda8")
                  .retrieve()
                  .toEntity(Map.class);

      assertThatExceptionOfType(HttpServerErrorException.class)
          .isThrownBy(call)
          .extracting(e -> e.getResponseBodyAs(Map.class))
          .extracting("status")
          .isEqualTo("DOWN");
    }
  }
}
