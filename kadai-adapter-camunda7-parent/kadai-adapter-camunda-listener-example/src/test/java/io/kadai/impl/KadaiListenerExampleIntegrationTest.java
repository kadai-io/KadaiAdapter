package io.kadai.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.camunda.bpm.engine.task.Task;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KadaiListenerExampleIntegrationTest {

  private ProcessEngine processEngine;

  @BeforeEach
  void setUp() {
    StandaloneProcessEngineConfiguration configuration = new StandaloneProcessEngineConfiguration();
    configuration.setJdbcUrl("jdbc:h2:mem:kadai-listener-example;DB_CLOSE_DELAY=-1");
    configuration.setJdbcDriver("org.h2.Driver");
    configuration.setJdbcUsername("sa");
    configuration.setJdbcPassword("");
    configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP);
    configuration.setDataSource(createDataSource());
    configuration.setProcessEnginePlugins(List.of(new KadaiParseListenerProcessEnginePlugin()));
    processEngine = configuration.buildProcessEngine();
    var deployment = processEngine.getRepositoryService().createDeployment();
    deployment.addClasspathResource("processes/simple_user_task_process.bpmn");
    deployment.deploy();
  }

  @AfterEach
  void tearDown() {
    if (processEngine != null) {
      processEngine.close();
    }
  }

  @Test
  void should_CreateOutboxEvent_When_ListenerExampleProcessStarts() throws Exception {
    String processInstanceId =
        processEngine
            .getRuntimeService()
            .startProcessInstanceByKey(
                "simple_user_task_process", Map.of("amount", 42, "item", "example-item"))
            .getProcessInstanceId();

    Task camundaTask =
        processEngine
            .getTaskService()
            .createTaskQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

    Map<String, String> event = loadOutboxEvent(camundaTask.getId());

    assertThat(event)
        .containsEntry("TYPE", "create")
        .containsEntry("CAMUNDA_TASK_ID", camundaTask.getId());
    assertThat(event.get("PAYLOAD"))
        .contains("\"id\":\"" + camundaTask.getId() + "\"")
        .contains("\"domain\":\"DOMAIN_A\"")
        .contains("\"classificationKey\":\"L1050\"");
  }

  private Map<String, String> loadOutboxEvent(String camundaTaskId) throws Exception {
    DataSource dataSource = processEngine.getProcessEngineConfiguration().getDataSource();
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "SELECT TYPE, PAYLOAD, CAMUNDA_TASK_ID FROM KADAI_TABLES.EVENT_STORE "
                    + "WHERE CAMUNDA_TASK_ID = ?")) {
      statement.setString(1, camundaTaskId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return Map.of(
            "TYPE", resultSet.getString("TYPE"),
            "PAYLOAD", resultSet.getString("PAYLOAD"),
            "CAMUNDA_TASK_ID", resultSet.getString("CAMUNDA_TASK_ID"));
      }
    }
  }

  private DataSource createDataSource() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:kadai-listener-example;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");
    return dataSource;
  }
}
