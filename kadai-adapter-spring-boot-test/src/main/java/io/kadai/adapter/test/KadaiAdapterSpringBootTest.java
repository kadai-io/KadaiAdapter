package io.kadai.adapter.test;

import io.kadai.common.test.security.JaasExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * High-Level annotation setting up Kadai for integration-tests with the KadaiAdapter. Bootstraps
 * the entire Spring-Context and adds additional beans for {@link io.kadai.common.api.KadaiEngine
 * KadaiEngine} and Co.
 *
 * <p>Auto-configures via {@link KadaiAdapterTestApplication}.
 *
 * <p>This annotation provides a neat way to test interplay of Kadai and KadaiAdapter (kernel) but
 * also allows <b>testing plugins</b>.
 *
 * @see KadaiAdapterSpringBootTestConfiguration
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({SpringExtension.class, JaasExtension.class})
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = KadaiAdapterSpringBootTestConfiguration.class)
public @interface KadaiAdapterSpringBootTest {}
