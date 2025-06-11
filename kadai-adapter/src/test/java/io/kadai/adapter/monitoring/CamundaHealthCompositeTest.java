package io.kadai.adapter.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class CamundaHealthCompositeTest {

  @Test
  void should_CreateAllContributingCamundaHealthIndicators() {
    final CamundaHealthComposite camundaHealthComposite =
        new CamundaHealthComposite(
            mock(),
            List.of(
                "http://localhost:8081/example-context-root/engine-rest",
                "http://localhost:8082/example-context-root/engine-rest"));

    assertThat(camundaHealthComposite.getContributor("camundaSystemRest1")).isNotNull();
    assertThat(camundaHealthComposite.getContributor("camundaSystemRest2")).isNotNull();
    long contributorCount =
        StreamSupport.stream(camundaHealthComposite.spliterator(), false).count();
    assertThat(contributorCount).isEqualTo(2);
  }
}
