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

package io.kadai.adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configures the adapter. */
@EnableScheduling
@Import({SchedulerConfiguration.class})
@Configuration
@EnableTransactionManagement
@ConfigurationProperties(prefix = "kadai-adapter.kernel")
public class AdapterConfiguration {

  private String runAsUser;
  private SchedulerConfig scheduler;

  public String getRunAsUser() {
    return runAsUser;
  }

  public void setRunAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
  }

  public SchedulerConfig getScheduler() {
    return scheduler;
  }

  public void setScheduler(SchedulerConfig scheduler) {
    this.scheduler = scheduler;
  }

  public static class SchedulerConfig {
    private long startKadaiTasksInterval = 5000L;
    private long completeReferencedTasksInterval = 5000L;
    private long claimReferencedTasksInterval = 5000L;
    private long cancelClaimReferencedTasksInterval = 5000L;
    private long checkFinishedReferencedTasksInterval = 5000L;
    private long retriesAndBlockingTaskEventsInterval = 10000L;

    public long getStartKadaiTasksInterval() {
      return startKadaiTasksInterval;
    }

    public void setStartKadaiTasksInterval(long startKadaiTasksInterval) {
      this.startKadaiTasksInterval = startKadaiTasksInterval;
    }

    public long getCompleteReferencedTasksInterval() {
      return completeReferencedTasksInterval;
    }

    public void setCompleteReferencedTasksInterval(long completeReferencedTasksInterval) {
      this.completeReferencedTasksInterval = completeReferencedTasksInterval;
    }

    public long getClaimReferencedTasksInterval() {
      return claimReferencedTasksInterval;
    }

    public void setClaimReferencedTasksInterval(long claimReferencedTasksInterval) {
      this.claimReferencedTasksInterval = claimReferencedTasksInterval;
    }

    public long getCancelClaimReferencedTasksInterval() {
      return cancelClaimReferencedTasksInterval;
    }

    public void setCancelClaimReferencedTasksInterval(long cancelClaimReferencedTasksInterval) {
      this.cancelClaimReferencedTasksInterval = cancelClaimReferencedTasksInterval;
    }

    public long getCheckFinishedReferencedTasksInterval() {
      return checkFinishedReferencedTasksInterval;
    }

    public void setCheckFinishedReferencedTasksInterval(long checkFinishedReferencedTasksInterval) {
      this.checkFinishedReferencedTasksInterval = checkFinishedReferencedTasksInterval;
    }

    public long getRetriesAndBlockingTaskEventsInterval() {
      return retriesAndBlockingTaskEventsInterval;
    }

    public void setRetriesAndBlockingTaskEventsInterval(long retriesAndBlockingTaskEventsInterval) {
      this.retriesAndBlockingTaskEventsInterval = retriesAndBlockingTaskEventsInterval;
    }
  }
}
