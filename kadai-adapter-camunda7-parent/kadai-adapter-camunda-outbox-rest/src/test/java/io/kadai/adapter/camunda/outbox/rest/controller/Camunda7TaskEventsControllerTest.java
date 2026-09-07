package io.kadai.adapter.camunda.outbox.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.camunda.outbox.rest.exception.OutboxServiceUnavailableException;
import io.kadai.adapter.camunda.outbox.rest.exception.OutboxServiceUnavailableExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.resource.OutboxEventCountResource;
import io.kadai.adapter.camunda.outbox.rest.service.Camunda7TaskEventsService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class Camunda7TaskEventsControllerTest {

  @Test
  void should_ReturnTypedEventCountResource_When_EventCountQuerySucceeds() {
    Camunda7TaskEventsController controller = new Camunda7TaskEventsController();
    controller.camunda7TaskEventService = eventCountServiceReturning(7);

    Response response = controller.getEventsCount(0);

    try {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getEntity()).isInstanceOf(OutboxEventCountResource.class);
      assertThat(((OutboxEventCountResource) response.getEntity()).getEventsCount()).isEqualTo(7);
    } finally {
      response.close();
    }
  }

  @Test
  void should_PropagateServiceUnavailable_When_EventCountQueryFails() {
    Camunda7TaskEventsController controller = new Camunda7TaskEventsController();
    controller.camunda7TaskEventService = eventCountServiceFailing();

    assertThatThrownBy(() -> controller.getEventsCount(0))
        .isInstanceOf(OutboxServiceUnavailableException.class);
  }

  @Test
  void should_MapServiceUnavailableTo503WithoutExposingCause() {
    Response response =
        new OutboxServiceUnavailableExceptionMapper()
            .toResponse(
                new OutboxServiceUnavailableException(
                    "Unable to retrieve Outbox event count", new IllegalStateException("secret")));

    try {
      assertThat(response.getStatus()).isEqualTo(503);
      assertThat(response.getEntity()).isEqualTo("Outbox persistence is unavailable");
    } finally {
      response.close();
    }
  }

  private static Camunda7TaskEventsService eventCountServiceReturning(int eventCount) {
    return new Camunda7TaskEventsService() {
      @Override
      public int getEventsCount(int remainingRetries) {
        return eventCount;
      }
    };
  }

  private static Camunda7TaskEventsService eventCountServiceFailing() {
    return new Camunda7TaskEventsService() {
      @Override
      public int getEventsCount(int remainingRetries) {
        throw new OutboxServiceUnavailableException("Unable to retrieve Outbox event count");
      }
    };
  }
}
