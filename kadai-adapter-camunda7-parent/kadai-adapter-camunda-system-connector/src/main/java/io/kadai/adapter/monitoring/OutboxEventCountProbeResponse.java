package io.kadai.adapter.monitoring;

final class OutboxEventCountProbeResponse {

  private Integer eventsCount;

  public Integer getEventsCount() {
    return eventsCount;
  }

  public void setEventsCount(Integer eventsCount) {
    this.eventsCount = eventsCount;
  }
}
