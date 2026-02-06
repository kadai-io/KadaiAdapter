package io.kadai.adapter.systemconnector.camunda;

import io.camunda.process.test.api.CamundaProcessTestExecutionListener;
import io.camunda.process.test.impl.configuration.CamundaProcessTestAutoConfiguration;
import io.kadai.adapter.test.KadaiAdapterSpringBootTestConfiguration;
import io.kadai.adapter.test.KadaiAdapterSpringBootTestExecutionListener;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * High-Level annotation setting up Kadai, KadaiAdapter and Camunda8 for integration-tests.
 *
 * <p>Meta-Annotation built on top of {@link
 * io.camunda.process.test.api.CamundaSpringProcessTest @CamundaSpringProcessTest}.
 *
 * @see KadaiAdapterSpringBootTestExecutionListener
 * @see CamundaProcessTestExecutionListener
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({SpringExtension.class, JaasExtension.class})
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = KadaiAdapterSpringBootTestConfiguration.class)
@Import({CamundaProcessTestAutoConfiguration.class})
@TestExecutionListeners(
    value = {
      KadaiAdapterSpringBootTestExecutionListener.class,
      CamundaProcessTestExecutionListener.class,
      Camunda8TestSetupListener.class
    },
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource("classpath:camunda8-test-application.properties")
public @interface KadaiAdapterCamunda8SpringBootTest {}
