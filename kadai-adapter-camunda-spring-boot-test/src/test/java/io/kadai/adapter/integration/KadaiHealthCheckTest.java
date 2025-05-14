package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.KadaiHealthCheck;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
class KadaiHealthCheckTest {

  @Spy private KadaiHealthCheck kadaiHealthCheckSpy;

  @Test
  void should_ReturnUp_When_ReturnValidSchemaVersion() {
    when(kadaiHealthCheckSpy.getCurrentSchemaVersion()).thenReturn("1.0.0");

    Health health = Health.up().withDetail("Kadai Version", "1.0.0").build();
    assertThat(kadaiHealthCheckSpy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_ExceptionThrownForSchemaVersion() {
    Mockito.doThrow(new RuntimeException("Simulated exception"))
        .when(kadaiHealthCheckSpy)
        .getCurrentSchemaVersion();

    Health health = Health.down().withDetail("Kadai Service Error", "Simulated exception").build();
    assertThat(kadaiHealthCheckSpy.health()).isEqualTo(health);
  }
}
