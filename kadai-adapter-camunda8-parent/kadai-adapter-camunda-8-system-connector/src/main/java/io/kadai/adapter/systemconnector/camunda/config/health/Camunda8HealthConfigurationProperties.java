package io.kadai.adapter.systemconnector.camunda.config.health;

import io.kadai.adapter.configuration.health.CompositeHealthContributorConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "management.health.kadai-adapter.plugin.camunda8")
public class Camunda8HealthConfigurationProperties
    extends CompositeHealthContributorConfigurationProperties {

  private JobWorkerHealthConfigurationProperties jobWorker =
      new JobWorkerHealthConfigurationProperties();

  public JobWorkerHealthConfigurationProperties getJobWorker() {
    return jobWorker;
  }

  public void setJobWorker(JobWorkerHealthConfigurationProperties jobWorker) {
    this.jobWorker = jobWorker;
  }

  public static class JobWorkerHealthConfigurationProperties
      extends CompositeHealthContributorConfigurationProperties {

    private CompositeHealthContributorConfigurationProperties complete =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties create =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties cancel =
        new CompositeHealthContributorConfigurationProperties();

    public CompositeHealthContributorConfigurationProperties getComplete() {
      return complete;
    }

    public void setComplete(CompositeHealthContributorConfigurationProperties complete) {
      this.complete = complete;
    }

    public JobWorkerHealthConfigurationProperties withComplete(
        CompositeHealthContributorConfigurationProperties complete) {
      this.complete = complete;
      return this;
    }

    public CompositeHealthContributorConfigurationProperties getCreate() {
      return create;
    }

    public void setCreate(CompositeHealthContributorConfigurationProperties create) {
      this.create = create;
    }

    public JobWorkerHealthConfigurationProperties withCreate(
        CompositeHealthContributorConfigurationProperties create) {
      this.create = create;
      return this;
    }

    public CompositeHealthContributorConfigurationProperties getCancel() {
      return cancel;
    }

    public void setCancel(CompositeHealthContributorConfigurationProperties cancel) {
      this.cancel = cancel;
    }

    public JobWorkerHealthConfigurationProperties withCancel(
        CompositeHealthContributorConfigurationProperties cancel) {
      this.cancel = cancel;
      return this;
    }
  }
}
