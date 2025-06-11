package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class OutboxHealthCompositeTest {

  @Test
  void should_CreateAllContributingOutboxHealthIndicators() {
    final OutboxHealthComposite outboxHealthComposite =
        new OutboxHealthComposite(
            mock(),
            List.of(
                "http://localhost:8081/example-context-root/outbox-rest",
                "http://localhost:8082/example-context-root/outbox-rest"));

    assertThat(outboxHealthComposite.getContributor("outboxRest1")).isNotNull();
    assertThat(outboxHealthComposite.getContributor("outboxRest2")).isNotNull();
    long contributorCount =
        StreamSupport.stream(outboxHealthComposite.spliterator(), false).count();
    assertThat(contributorCount).isEqualTo(2);
  }
}
