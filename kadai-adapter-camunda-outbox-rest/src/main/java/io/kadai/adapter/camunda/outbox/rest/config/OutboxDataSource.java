package io.kadai.adapter.camunda.outbox.rest.config;

import io.kadai.adapter.camunda.OutboxRestConfiguration;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDataSource.class);
  private static volatile DataSource dataSource;

  private OutboxDataSource() {}

  public static DataSource get() {
    DataSource local = dataSource;
    if (local == null) {
      synchronized (OutboxDataSource.class) {
        local = dataSource;
        if (local == null) {
          dataSource = local = getDataSourceFromPropertiesFile();
        }
      }
    }
    return local;
  }

  private static DataSource getDataSourceFromPropertiesFile() {
    String jndiUrl = OutboxRestConfiguration.getOutboxDatasourceJndi();
    if (jndiUrl != null) {
      try {
        return (DataSource) new InitialContext().lookup(jndiUrl);
      } catch (NamingException e) {
        LOGGER.error("Failed creating initial context. Rethrowing as RuntimeException.", e);
        throw new RuntimeException(e);
      }
    } else {
      return new PooledDataSource(
          OutboxRestConfiguration.getOutboxDatasourceDriver(),
          OutboxRestConfiguration.getOutboxDatasourceUrl(),
          OutboxRestConfiguration.getOutboxDatasourceUsername(),
          OutboxRestConfiguration.getOutboxDatasourcePassword());
    }
  }
}
