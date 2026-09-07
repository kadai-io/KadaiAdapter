package io.kadai.adapter.monitoring.models;

public class OutboxEventCountRepresentationModel {

  private Integer eventsCount;

  public Integer getEventsCount() {
    return eventsCount;
  }

  public void setEventsCount(Integer eventsCount) {
    this.eventsCount = eventsCount;
  }
}
