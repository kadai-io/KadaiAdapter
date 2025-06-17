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

  public ExternalServicesHealthConfigurationProperties withCamunda(
      CompositeHealthContributorConfigurationProperties camunda) {
    this.camunda = camunda;
    return this;
  }

  public ExternalServicesHealthConfigurationProperties withOutbox(
      CompositeHealthContributorConfigurationProperties outbox) {
    this.outbox = outbox;
    return this;
  }

  public ExternalServicesHealthConfigurationProperties withKadai(
      CompositeHealthContributorConfigurationProperties kadai) {
    this.kadai = kadai;
    return this;
  }

  public ExternalServicesHealthConfigurationProperties withScheduler(
      SchedulerHealthConfigurationProperties scheduler) {
    this.scheduler = scheduler;
    return this;
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

    /**
     * The factor the expected run-time is multiplied with before checking for the next expected
     * run.
     *
     * <p>The higher the value, the less strict.
     */
    private long runTimeAcceptanceMultiplier = 2L;

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

    public SchedulerHealthConfigurationProperties withReferencedTaskCompleter(
        CompositeHealthContributorConfigurationProperties referencedTaskCompleter) {
      this.referencedTaskCompleter = referencedTaskCompleter;
      return this;
    }

    public SchedulerHealthConfigurationProperties withReferencedTaskClaimer(
        CompositeHealthContributorConfigurationProperties referencedTaskClaimer) {
      this.referencedTaskClaimer = referencedTaskClaimer;
      return this;
    }

    public SchedulerHealthConfigurationProperties withReferencedTaskClaimCanceler(
        CompositeHealthContributorConfigurationProperties referencedTaskClaimCanceler) {
      this.referencedTaskClaimCanceler = referencedTaskClaimCanceler;
      return this;
    }

    public SchedulerHealthConfigurationProperties withKadaiTaskStarter(
        CompositeHealthContributorConfigurationProperties kadaiTaskStarter) {
      this.kadaiTaskStarter = kadaiTaskStarter;
      return this;
    }

    public SchedulerHealthConfigurationProperties withKadaiTaskTerminator(
        CompositeHealthContributorConfigurationProperties kadaiTaskTerminator) {
      this.kadaiTaskTerminator = kadaiTaskTerminator;
      return this;
    }

    public long getRunTimeAcceptanceMultiplier() {
      return runTimeAcceptanceMultiplier;
    }

    public void setRunTimeAcceptanceMultiplier(long runTimeAcceptanceMultiplier) {
      this.runTimeAcceptanceMultiplier = runTimeAcceptanceMultiplier;
    }
  }
}
