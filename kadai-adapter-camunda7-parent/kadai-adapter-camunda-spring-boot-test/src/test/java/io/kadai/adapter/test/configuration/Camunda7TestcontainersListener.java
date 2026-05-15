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
 */

package io.kadai.adapter.test.configuration;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Boots the singleton Camunda BPM Run + PostgreSQL test containers exactly once per JUnit Launcher
 * session — i.e. once per Surefire JVM, before any test class is discovered or loaded. This
 * guarantees the system properties published by {@link Camunda7TestcontainersConfiguration} are
 * visible to every Spring application context created by any subsequent {@code @SpringBootTest},
 * regardless of whether the test class extends {@code AbsIntegrationTest} or not.
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class Camunda7TestcontainersListener implements LauncherSessionListener {

  @Override
  public void launcherSessionOpened(LauncherSession session) {
    Camunda7TestcontainersConfiguration.initialize();
  }

  @Override
  public void launcherSessionClosed(LauncherSession session) {
    // Containers stay up for the lifetime of the JVM — Testcontainers' Ryuk reaper
    // shuts them down when the JVM exits.
  }
}
