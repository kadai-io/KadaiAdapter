package io.kadai.adapter.configuration.health;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "management.health.external-services")
public class ExternalServicesHealthConfigurationProperties
    extends CompositeHealthContributorConfigurationProperties {

  private CompositeHealthContributorConfigurationProperties camunda =
      new CompositeHealthContributorConfigurationProperties();
  private CompositeHealthContributorConfigurationProperties outbox =
      new CompositeHealthContributorConfigurationProperties();
  private CompositeHealthContributorConfigurationProperties kadai =
      new CompositeHealthContributorConfigurationProperties();
  private SchedulerHealthConfigurationProperties scheduler =
      new SchedulerHealthConfigurationProperties();

  public CompositeHealthContributorConfigurationProperties getCamunda() {
    return camunda;
  }

  public void setCamunda(CompositeHealthContributorConfigurationProperties camunda) {
    this.camunda = camunda;
  }

  public CompositeHealthContributorConfigurationProperties getOutbox() {
    return outbox;
  }

  public void setOutbox(CompositeHealthContributorConfigurationProperties outbox) {
    this.outbox = outbox;
  }

  public CompositeHealthContributorConfigurationProperties getKadai() {
    return kadai;
  }

  public void setKadai(CompositeHealthContributorConfigurationProperties kadai) {
    this.kadai = kadai;
  }

  public SchedulerHealthConfigurationProperties getScheduler() {
    return scheduler;
  }

  public void setScheduler(SchedulerHealthConfigurationProperties scheduler) {
    this.scheduler = scheduler;
  }

  public static class SchedulerHealthConfigurationProperties
      extends CompositeHealthContributorConfigurationProperties {

    private CompositeHealthContributorConfigurationProperties referencedTaskCompleter =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties referencedTaskClaimer =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties referencedTaskClaimCanceler =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties kadaiTaskStarter =
        new CompositeHealthContributorConfigurationProperties();
    private CompositeHealthContributorConfigurationProperties kadaiTaskTerminator =
        new CompositeHealthContributorConfigurationProperties();

    public CompositeHealthContributorConfigurationProperties getReferencedTaskCompleter() {
      return referencedTaskCompleter;
    }

    public void setReferencedTaskCompleter(
        CompositeHealthContributorConfigurationProperties referencedTaskCompleter) {
      this.referencedTaskCompleter = referencedTaskCompleter;
    }

    public CompositeHealthContributorConfigurationProperties getReferencedTaskClaimer() {
      return referencedTaskClaimer;
    }

    public void setReferencedTaskClaimer(
        CompositeHealthContributorConfigurationProperties referencedTaskClaimer) {
      this.referencedTaskClaimer = referencedTaskClaimer;
    }

    public CompositeHealthContributorConfigurationProperties getReferencedTaskClaimCanceler() {
      return referencedTaskClaimCanceler;
    }

    public void setReferencedTaskClaimCanceler(
        CompositeHealthContributorConfigurationProperties referencedTaskClaimCanceler) {
      this.referencedTaskClaimCanceler = referencedTaskClaimCanceler;
    }

    public CompositeHealthContributorConfigurationProperties getKadaiTaskStarter() {
      return kadaiTaskStarter;
    }

    public void setKadaiTaskStarter(
        CompositeHealthContributorConfigurationProperties kadaiTaskStarter) {
      this.kadaiTaskStarter = kadaiTaskStarter;
    }

    public CompositeHealthContributorConfigurationProperties getKadaiTaskTerminator() {
      return kadaiTaskTerminator;
    }

    public void setKadaiTaskTerminator(
        CompositeHealthContributorConfigurationProperties kadaiTaskTerminator) {
      this.kadaiTaskTerminator = kadaiTaskTerminator;
    }
  }
}
