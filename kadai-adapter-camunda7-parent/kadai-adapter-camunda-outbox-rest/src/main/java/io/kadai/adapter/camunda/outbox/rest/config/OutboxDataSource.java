package io.kadai.adapter.camunda.outbox.rest.config;

import io.kadai.adapter.camunda.OutboxRestConfiguration;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;

public final class OutboxDataSource {

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
    return new PooledDataSource(
        OutboxRestConfiguration.getOutboxDatasourceDriver(),
        OutboxRestConfiguration.getOutboxDatasourceUrl(),
        OutboxRestConfiguration.getOutboxDatasourceUsername(),
        OutboxRestConfiguration.getOutboxDatasourcePassword());
  }
}
