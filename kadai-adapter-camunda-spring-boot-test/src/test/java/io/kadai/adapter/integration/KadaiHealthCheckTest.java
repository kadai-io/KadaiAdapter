package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.KadaiHealthCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

class KadaiHealthCheckTest {

  private KadaiHealthCheck kadaiHealthCheckSpy;

  @BeforeEach
  void setUp() {
    this.kadaiHealthCheckSpy = Mockito.spy(new KadaiHealthCheck());
  }

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
