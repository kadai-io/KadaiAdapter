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

package io.kadai.adapter.kadaiconnector.config;

import io.kadai.KadaiConfiguration;
import io.kadai.classification.api.ClassificationService;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.internal.SpringKadaiEngine;
import io.kadai.task.api.TaskService;
import io.kadai.workbasket.api.WorkbasketService;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configuration for KADAI task system connector. */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
@EnableTransactionManagement
@ConfigurationProperties(prefix = "kadai-adapter.kernel.kadai-connector")
public class KadaiSystemConnectorConfiguration {

  @Value("${kadai.schemaName:KADAI}")
  public String kadaiSchemaName;

  /** Configuration for mapping Kadai-Tasks. */
  private TaskMappingConfiguration taskMapping = new TaskMappingConfiguration();

  /** Amount of tasks to sync from Kadai to external systems in a single run. */
  private Integer batchSize = 64;

  @Value("${kadai.datasource.jndi-name:no-jndi-configured}")
  private String jndiName;

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public TaskMappingConfiguration getTaskMapping() {
    return taskMapping;
  }

  public void setTaskMapping(TaskMappingConfiguration taskMapping) {
    this.taskMapping = taskMapping;
  }

  @Bean(name = "kadaiDataSource")
  @ConfigurationProperties(prefix = "kadai.datasource")
  public DataSource kadaiDataSource() throws NamingException {
    if ("no-jndi-configured".equals(jndiName)) {
      return DataSourceBuilder.create().build();
    } else {
      Context ctx = new InitialContext();
      return (DataSource) ctx.lookup(jndiName);
    }
  }

  @Bean
  public TaskService getTaskService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getTaskService();
  }

  @Bean
  public WorkbasketService getWorkbasketService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getWorkbasketService();
  }

  @Bean
  public ClassificationService getClassificationService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getClassificationService();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public SpringKadaiEngine kadaiEngine(KadaiConfiguration kadaiConfiguration) throws SQLException {
    return SpringKadaiEngine.buildKadaiEngine(kadaiConfiguration);
  }

  @Bean
  @ConditionalOnMissingBean(KadaiConfiguration.class)
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public KadaiConfiguration kadaiConfiguration(
      @Qualifier("kadaiDataSource") DataSource kadaiDataSource,
      @Qualifier("kadaiPropertiesFileName") String propertiesFileName,
      @Qualifier("kadaiPropertiesDelimiter") String delimiter) {
    return new KadaiConfiguration.Builder(kadaiDataSource, true, kadaiSchemaName, true)
        .initKadaiProperties(propertiesFileName, delimiter)
        .build();
  }

  @Bean
  public String kadaiPropertiesFileName() {
    return "/kadai.properties";
  }

  @Bean
  public String kadaiPropertiesDelimiter() {
    return "|";
  }

  public static class TaskMappingConfiguration {

    /** Configuration for mapping Object-References in Kadai-Tasks. */
    private TaskMappingObjectReferenceConfiguration objectReference =
        new TaskMappingObjectReferenceConfiguration();

    public TaskMappingObjectReferenceConfiguration getObjectReference() {
      return objectReference;
    }

    public void setObjectReference(TaskMappingObjectReferenceConfiguration objectReference) {
      this.objectReference = objectReference;
    }

    public static class TaskMappingObjectReferenceConfiguration {

      /** Default Object-Reference-Company used for mapping of external tasks to Kadai-Tasks. */
      private String company = "DEFAULT_COMPANY";

      /** Default Object-Reference-System used for mapping of external tasks to Kadai-Tasks. */
      private String system = "DEFAULT_SYSTEM";

      /**
       * Default Object-Reference-System-Instance used for mapping of external tasks to Kadai-Tasks.
       */
      private String systemInstance = "DEFAULT_SYSTEM_INSTANCE";

      /** Default Object-Reference-Type used for mapping of external tasks to Kadai-Tasks. */
      private String type = "DEFAULT_TYPE";

      /** Default Object-Reference-Value used for mapping of external tasks to Kadai-Tasks. */
      private String value = "DEFAULT_VALUE";

      public String getCompany() {
        return company;
      }

      public void setCompany(String company) {
        this.company = company;
      }

      public String getSystem() {
        return system;
      }

      public void setSystem(String system) {
        this.system = system;
      }

      public String getSystemInstance() {
        return systemInstance;
      }

      public void setSystemInstance(String systemInstance) {
        this.systemInstance = systemInstance;
      }

      public String getType() {
        return type;
      }

      public void setType(String type) {
        this.type = type;
      }

      public String getValue() {
        return value;
      }

      public void setValue(String value) {
        this.value = value;
      }
    }
  }
}
