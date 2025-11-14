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

package io.kadai.adapter.camunda;

import io.kadai.adapter.camunda.exceptions.SystemException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamundaListenerConfiguration {

  private static final String KADAI_ADAPTER_CREATE_OUTBOX_SCHEMA =
      "kadai.adapter.create_outbox_schema";
  private static final String KADAI_OUTBOX_PROPERTIES = "kadai-outbox.properties";
  private static final String KADAI_ADAPTER_OUTBOX_SCHEMA = "kadai.adapter.outbox.schema";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_JNDI =
      "kadai.adapter.outbox.datasource.jndi";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_DRIVER =
      "kadai.adapter.outbox.datasource.driver";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_URL =
      "kadai.adapter.outbox.datasource.url";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_USERNAME =
      "kadai.adapter.outbox.datasource.username";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_PASSWORD =
      "kadai.adapter.outbox.datasource.password";
  private static final String KADAI_ADAPTER_OUTBOX_INITIAL_NUMBER_OF_TASK_CREATION_RETRIES =
      "kadai.adapter.outbox.initial.number.of.task.creation.retries";
  private static final String OUTBOX_SYSTEM_PROPERTY = "kadai.outbox.properties";
  private static final String OUTBOX_SCHEMA_DEFAULT = "kadai_tables";
  private static final String EXCEPTION_FOR_FAULTY_PROCESS_VARIABLES =
      "kadai.listener.process.variables.exception";
  private static final boolean CREATE_OUTBOX_SCHEMA_DEFAULT = true;
  private static final int INITIAL_NUMBER_OF_TASK_CREATION_RETRIES_DEFAULT = 5;
  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaListenerConfiguration.class);
  private final Properties outboxProperties = new Properties();

  private CamundaListenerConfiguration() {

    String outboxPropertiesFile = System.getProperty(OUTBOX_SYSTEM_PROPERTY);

    if (outboxPropertiesFile != null) {

      try (FileInputStream propertiesStream = new FileInputStream(outboxPropertiesFile)) {

        outboxProperties.load(propertiesStream);

        LOGGER.info("Outbox properties were loaded from file {}.", outboxPropertiesFile);

      } catch (Exception e) {
        LOGGER.warn(
            "Caught Exception while trying to load properties from "
                + "provided properties file {}. "
                + "Trying to read properties from classpath",
            outboxPropertiesFile,
            e);

        readPropertiesFromClasspath();
      }
    } else {
      readPropertiesFromClasspath();
    }
  }

  public static CamundaListenerConfiguration getInstance() {
    return CamundaListenerConfiguration.LazyHolder.INSTANCE;
  }

  public static boolean getCreateOutboxSchema() {

    String createOutboxSchemaProperty =
        getInstance().outboxProperties.getProperty(KADAI_ADAPTER_CREATE_OUTBOX_SCHEMA);

    if ((createOutboxSchemaProperty == null) || createOutboxSchemaProperty.isEmpty()) {
      LOGGER.info(
          "Couldn't retrieve boolean property to create schema or not, setting to default ");
      return CREATE_OUTBOX_SCHEMA_DEFAULT;
    } else {
      return !"false".equalsIgnoreCase(createOutboxSchemaProperty);
    }
  }

  public static String getOutboxSchema() {

    String outboxSchema = getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_SCHEMA);

    if (outboxSchema == null || outboxSchema.isEmpty()) {
      LOGGER.info("Couldn't retrieve property entry for outbox schema, setting to default ");
      return OUTBOX_SCHEMA_DEFAULT;

    } else {
      return outboxSchema;
    }
  }

  public static String getOutboxDatasourceJndi() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_JNDI);
  }

  public static String getOutboxDatasourceDriver() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_DRIVER);
  }

  public static String getOutboxDatasourceUrl() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_URL);
  }

  public static String getOutboxDatasourceUsername() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_USERNAME);
  }

  public static String getOutboxDatasourcePassword() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_PASSWORD);
  }

  public static boolean shouldCatchAndLogExceptionForFaultyProcessVariables() {
    return Boolean.parseBoolean(
        getInstance().outboxProperties.getProperty(EXCEPTION_FOR_FAULTY_PROCESS_VARIABLES));
  }

  public static int getInitialNumberOfTaskCreationRetries() {
    int initialNumberOfTaskCreationRetries;

    String initialNumberOfTaskCreationRetriesProperty =
        getInstance()
            .outboxProperties
            .getProperty(KADAI_ADAPTER_OUTBOX_INITIAL_NUMBER_OF_TASK_CREATION_RETRIES);

    try {
      initialNumberOfTaskCreationRetries =
          Integer.parseInt(initialNumberOfTaskCreationRetriesProperty);
    } catch (NumberFormatException e) {
      initialNumberOfTaskCreationRetries = INITIAL_NUMBER_OF_TASK_CREATION_RETRIES_DEFAULT;
      LOGGER.warn(
          "Attempted to retrieve initial number of task creation retries and caught "
              + "Exception. Setting default for initial number of "
              + "task creation retries to {} ",
          initialNumberOfTaskCreationRetries,
          e);
    }

    return initialNumberOfTaskCreationRetries;
  }

  private void readPropertiesFromClasspath() {

    try (InputStream propertiesStream =
        this.getClass().getClassLoader().getResourceAsStream(KADAI_OUTBOX_PROPERTIES)) {

      outboxProperties.load(propertiesStream);

      LOGGER.debug(
          "Outbox properties were loaded from file {} from classpath.", KADAI_OUTBOX_PROPERTIES);
    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to load properties from file {} from classpath",
          KADAI_OUTBOX_PROPERTIES,
          e);
      throw new SystemException(
          String.format(
              "Internal System error when processing properties file %s ", KADAI_OUTBOX_PROPERTIES),
          e.getCause());
    }
  }

  private static class LazyHolder {
    private static final CamundaListenerConfiguration INSTANCE = new CamundaListenerConfiguration();
  }
}
