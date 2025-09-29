package io.kadai.adapter.test;

import io.kadai.common.test.security.JaasExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({SpringExtension.class, JaasExtension.class})
@SpringBootTest(classes = KadaiAdapterTestApplication.class)
@ContextConfiguration(classes = KadaiAdapterSpringBootTestConfiguration.class)
@TestExecutionListeners(value = {KadaiAdapterSpringBootTestExecutionListener.class})
public @interface KadaiAdapterSpringBootTest {}
