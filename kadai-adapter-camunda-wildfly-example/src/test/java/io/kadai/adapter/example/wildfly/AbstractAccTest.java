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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractAccTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAccTest.class);

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(AbstractAccTest::stopPostgresDb));

    startPostgresDb();
  }

  private static void startPostgresDb() {
    try {
      boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
      ProcessBuilder builder = new ProcessBuilder();
      if (isWindows) {
        builder.command(
            "cmd.exe",
            "/c",
            "docker compose -f ../docker-databases/docker-compose.yml up -d "
                + "kadai-postgres_14");
      } else {
        builder.command(
            "sh",
            "-c",
            "docker compose -f ../docker-databases/docker-compose.yml up -d "
                + "kadai-postgres_14");
      }
      Process process = builder.start();
      LOGGER.info("Starting POSTGRES...");
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("could not start postgres db!");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void stopPostgresDb() {
    try {
      boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
      ProcessBuilder builder = new ProcessBuilder();
      if (isWindows) {
        builder.command(
            "cmd.exe", "/c", "docker compose -f ../docker-databases/docker-compose.yml down -v");
      } else {
        builder.command(
            "sh", "-c", "docker compose -f ../docker-databases/docker-compose.yml down -v");
      }
      Process process = builder.start();
      LOGGER.info("Stopping POSTGRES...");
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("could not start postgres db!");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  String parseServerLog() throws Exception {

    // TO-DO: make log4j log into rollingFile from log4j.xml
    File file = new File("target/wildfly-31.0.1.Final/standalone/log/server.log");

    BufferedReader br = new BufferedReader(new FileReader(file));

    String str;
    StringBuilder stringBuilder = new StringBuilder();
    while ((str = br.readLine()) != null) {
      stringBuilder.append(str);
    }
    return stringBuilder.toString();
  }
}
