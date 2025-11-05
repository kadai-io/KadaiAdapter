package io.kadai.adapter.systemconnector.camunda.tasklistener;

import io.camunda.process.test.api.CamundaProcessTestExecutionListener;
import io.camunda.process.test.impl.configuration.CamundaProcessTestAutoConfiguration;

import io.kadai.adapter.systemconnector.camunda.api.impl.Camunda8TestSetupListener;
import io.kadai.adapter.test.KadaiAdapterSpringBootTest;
import io.kadai.adapter.test.KadaiAdapterSpringBootTestExecutionListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;

/**
 * High-Level annotation setting up Kadai, KadaiAdapter and Camunda8 for integration-tests.
 *
 * <p>Meta-Annotation built on top of {@link KadaiAdapterSpringBootTest @KadaiAdapterSpringBootTest}
 * and {@link io.camunda.process.test.api.CamundaSpringProcessTest @CamundaSpringProcessTest}.
 *
 * @see KadaiAdapterSpringBootTestExecutionListener
 * @see CamundaProcessTestExecutionListener
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@KadaiAdapterSpringBootTest
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
