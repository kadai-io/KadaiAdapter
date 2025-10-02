package io.kadai.adapter.test;

import com.zaxxer.hikari.HikariDataSource;
import io.kadai.KadaiConfiguration;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.api.KadaiEngine.ConnectionManagementMode;
import jakarta.annotation.Resource;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KadaiAdapterSpringBootTestConfiguration {

  @Resource(name = "kadaiDataSource")
  private DataSource kadaiDataSource;

  @Bean
  public KadaiConfiguration kadaiConfiguration() {
    String schema =
        ((HikariDataSource) kadaiDataSource).getSchema() == null
            ? "KADAI"
            : ((HikariDataSource) kadaiDataSource).getSchema();
    return new KadaiConfiguration.Builder(this.kadaiDataSource, false, schema)
        .initKadaiProperties()
        .build();
  }

  @Bean
  public KadaiEngine kadaiEngine(@Autowired KadaiConfiguration kadaiConfiguration)
      throws SQLException {
    return KadaiEngine.buildKadaiEngine(kadaiConfiguration, ConnectionManagementMode.AUTOCOMMIT);
  }

  @Bean
  public KadaiAdapterTestUtil kadaiAdapterTestUtil(@Autowired KadaiEngine kadaiEngine) {
    return new KadaiAdapterTestUtil(kadaiEngine);
  }
}
