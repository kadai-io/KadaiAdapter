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

package io.kadai.adapter.camunda.parselistener;

import io.kadai.adapter.camunda.Camunda7ListenerConfiguration;
import io.kadai.adapter.camunda.exceptions.SystemException;
import io.kadai.adapter.camunda.schemacreator.DB;
import io.kadai.adapter.camunda.schemacreator.KadaiOutboxSchemaCreator;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camunda engine plugin responsible for adding the KadaiParseListener to the
 * ProcessEngineConfguration, as well as initializing the outbox tables.
 */
public class KadaiParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin {

  private static final String OUTBOX_SCHEMA_VERSION = "1.12.0";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(KadaiParseListenerProcessEnginePlugin.class);

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    initParseListeners(processEngineConfiguration);
    initOutbox(processEngineConfiguration);
  }

  private void initParseListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {

    try {

      List<BpmnParseListener> preParseListeners =
          processEngineConfiguration.getCustomPreBPMNParseListeners();

      if (preParseListeners == null) {
        preParseListeners = new ArrayList<>();
        processEngineConfiguration.setCustomPreBPMNParseListeners(preParseListeners);
      }
      preParseListeners.add(new KadaiParseListener());

      LOGGER.debug("KadaiParseListener registered successfully");

    } catch (Exception e) {

      LOGGER.warn("Caught exception while trying to register KadaiParseListener", e);

      throw new SystemException(
          "An error occured while trying to register the KadaiParseListener."
              + " Aborting the boot of camunda.");
    }
  }

  private void initOutbox(ProcessEngineConfigurationImpl processEngineConfiguration) {

    DataSource camundaDataSource = retrieveCamundaDatasource(processEngineConfiguration);

    createSchema(camundaDataSource);

    LOGGER.info("KadaiOutbox initialized successfully");
  }

  private void createSchema(DataSource camundaDataSource) {

    String outboxSchema = initSchemaName(camundaDataSource);

    KadaiOutboxSchemaCreator schemaCreator =
        new KadaiOutboxSchemaCreator(camundaDataSource, outboxSchema);

    boolean isSchemaPreexisting = schemaCreator.isSchemaPreexisting();

    boolean shouldSchemaBeCreated = Camunda7ListenerConfiguration.getCreateOutboxSchema();

    if (!isSchemaPreexisting) {
      if (shouldSchemaBeCreated) {
        LOGGER.debug("Running scripts to create schema and tables for KadaiOutbox");
        if (!schemaCreator.createSchema()) {
          LOGGER.error(
              "An error occured while trying to automatically create the "
                  + "KadaiOutbox schema and table. "
                  + "Aborting the boot of camunda.");
          throw new SystemException(
              "An error occured while trying to automatically create the"
                  + " KadaiOutbox schema and table. "
                  + "Aborting the boot of camunda.");
        }
      } else {
        LOGGER.error("KadaiOutbox schema does not exist and shall not be created.");
        throw new SystemException("KadaiOutbox schema does not exist and shall not be created.");
      }
    }

    if (!schemaCreator.isValidSchemaVersion(OUTBOX_SCHEMA_VERSION)) {

      LOGGER.warn(
          "Aborting start up of camunda. "
              + "Please migrate to the newest version of the KadaiOutbox schema");
      throw new SystemException(
          "The Database Schema Version doesn't match the expected version "
              + OUTBOX_SCHEMA_VERSION
              + ". Aborting the boot of camunda.");
    }
  }

  private String initSchemaName(DataSource dataSource) {

    String outboxSchema = Camunda7ListenerConfiguration.getOutboxSchema();

    try (Connection connection = dataSource.getConnection()) {
      String databaseProductName = connection.getMetaData().getDatabaseProductName();
      if (DB.isPostgreSql(databaseProductName)) {
        outboxSchema = outboxSchema.toLowerCase();
      } else {
        outboxSchema = outboxSchema.toUpperCase();
      }
    } catch (SQLException ex) {
      LOGGER.error("Caught exception when attempting to initialize the schema name", ex);
    }

    LOGGER.debug("Using schema name {}", outboxSchema);

    return outboxSchema;
  }

  private DataSource retrieveCamundaDatasource(
      ProcessEngineConfigurationImpl processEngineConfiguration) {

    DataSource dataSource = processEngineConfiguration.getDataSource();

    if (dataSource == null) {
      LOGGER.warn(
          "ProcessEngineConfiguration returns null DataSource. "
              + "Retrieving DataSource from properties.");
      dataSource = getDataSourceFromPropertiesFile();
      if (dataSource == null) {
        LOGGER.warn(
            "getDataSourceFromPropertiesFile returns null. "
                + "Outbox tables must be initialized manually.");
        throw new MissingResourceException(
            "could not retrieve dataSource to initialize the outbox tables.",
            "KadaiOutboxSchemaCreator",
            "KadaiOutboxSchema");
      }
    }
    return dataSource;
  }

  private DataSource getDataSourceFromPropertiesFile() {
    DataSource dataSource = null;
    try {
      String jndiLookup = Camunda7ListenerConfiguration.getOutboxDatasourceJndi();

      if (jndiLookup != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiLookup);
        if (dataSource != null) {
          LOGGER.info("retrieved Datasource from jndi lookup {}", jndiLookup);
        } else {
          LOGGER.info("jndi lookup {} didn't return a Datasource.", jndiLookup);
        }
      } else {
        String driver = Camunda7ListenerConfiguration.getOutboxDatasourceDriver();
        String jdbcUrl = Camunda7ListenerConfiguration.getOutboxDatasourceUrl();
        String userName = Camunda7ListenerConfiguration.getOutboxDatasourceUsername();
        String password = Camunda7ListenerConfiguration.getOutboxDatasourcePassword();
        dataSource = createDatasource(driver, jdbcUrl, userName, password);
        LOGGER.info("created Datasource from properties {}, ...", jdbcUrl);
      }

    } catch (NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }
    return dataSource;
  }

  private static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }
}
