package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kadai.adapter.monitoring.KadaiHealthIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

class KadaiHealthIndicatorTest {

  private KadaiHealthIndicator kadaiHealthIndicatorSpy;

  @BeforeEach
  void setUp() {
    this.kadaiHealthIndicatorSpy = Mockito.spy(new KadaiHealthIndicator());
  }

  @Test
  void should_ReturnUp_When_ReturnValidSchemaVersion() {
    when(kadaiHealthIndicatorSpy.getCurrentSchemaVersion()).thenReturn("1.0.0");

    Health health = Health.up().withDetail("Kadai Version", "1.0.0").build();
    assertThat(kadaiHealthIndicatorSpy.health()).isEqualTo(health);
  }

  @Test
  void should_ReturnDown_When_ExceptionThrownForSchemaVersion() {
    Mockito.doThrow(new RuntimeException("Simulated exception"))
        .when(kadaiHealthIndicatorSpy)
        .getCurrentSchemaVersion();

    Health health = Health.down().withDetail("Kadai Service Error", "Simulated exception").build();
    assertThat(kadaiHealthIndicatorSpy.health()).isEqualTo(health);
  }
}
