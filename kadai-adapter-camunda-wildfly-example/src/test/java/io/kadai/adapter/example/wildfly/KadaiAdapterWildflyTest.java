/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.example.wildfly;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** This test class is configured to run with postgres DB. */
@Disabled(
    "Has already been removed with v10.0.0, but we're currently changing the past here with 9.3.1, "
        + "therefore just ignore failing tests.")
@ExtendWith(ArquillianExtension.class)
class KadaiAdapterWildflyTest extends AbstractAccTest {

  @Deployment(testable = false)
  static Archive<?> createTestArchive() {
    File[] files =
        Maven.resolver()
            .loadPomFromFile("pom.xml")
            .importCompileAndRuntimeDependencies()
            .resolve()
            .withTransitivity()
            .asFile();

    return ShrinkWrap.create(WebArchive.class, "KadaiAdapter.war")
        .addPackages(true, "io.kadai")
        .addAsResource("kadai.properties")
        .addAsResource("application.properties")
        .addAsWebInfResource("int-test-web.xml", "web.xml")
        .addAsWebInfResource("int-test-jboss-web.xml", "jboss-web.xml")
        .addAsLibraries(files);
  }

  @Test
  @RunAsClient
  void should_HaveConnectionErrorInLogs_When_ApplicationIsDeployedCorrectly() throws Exception {
    assertThat(parseServerLog())
        .contains(
            "Caught exception while trying to retrieve CamundaTaskEvents"
                + " from system with URL http://localhost:7001");
  }
}
