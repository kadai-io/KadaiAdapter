package io.kadai.adapter.systemconnector.camunda;

import io.camunda.process.test.api.CamundaProcessTestExecutionListener;
import io.camunda.process.test.impl.configuration.CamundaProcessTestAutoConfiguration;
import io.kadai.adapter.test.KadaiAdapterSpringBootTestConfiguration;
import io.kadai.adapter.test.KadaiAdapterSpringBootTestExecutionListener;
import io.kadai.common.test.security.JaasExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration-test setup for a runnable Camunda 8 adapter example.
 *
 * <p>Unlike {@link KadaiAdapterCamunda8SpringBootTest}, this annotation lets Spring Boot locate
 * the example application's {@code @SpringBootApplication}. It therefore verifies the same
 * configuration that users run, while providing the embedded KADAI database and Camunda process
 * test environment needed by the test.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({SpringExtension.class, JaasExtension.class})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({KadaiAdapterSpringBootTestConfiguration.class, CamundaProcessTestAutoConfiguration.class})
@TestExecutionListeners(
    value = {
      KadaiAdapterSpringBootTestExecutionListener.class,
      CamundaProcessTestExecutionListener.class,
      Camunda8TestSetupListener.class
    },
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public @interface KadaiAdapterCamunda8ExampleTest {}
