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

package io.kadai.adapter.camunda.outbox.rest.controller;

import io.kadai.adapter.camunda.outbox.rest.exception.Camunda7TaskEventNotFoundException;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEvent;
import io.kadai.adapter.camunda.outbox.rest.model.Camunda7TaskEventList;
import io.kadai.adapter.camunda.outbox.rest.resource.Camunda7TaskEventListResource;
import io.kadai.adapter.camunda.outbox.rest.resource.Camunda7TaskEventListResourceAssembler;
import io.kadai.adapter.camunda.outbox.rest.resource.Camunda7TaskEventResource;
import io.kadai.adapter.camunda.outbox.rest.resource.Camunda7TaskEventResourceAssembler;
import io.kadai.adapter.camunda.outbox.rest.service.Camunda7TaskEventsService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import spinjar.com.fasterxml.jackson.core.JsonProcessingException;
import spinjar.com.fasterxml.jackson.core.type.TypeReference;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

/** Controller for the Outbox REST service. */
@Path(Mapping.URL_EVENTS)
public class Camunda7TaskEventsController {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String REMAINING_RETRIES = "remainingRetries";

  Camunda7TaskEventsService camundaTaskEventService = new Camunda7TaskEventsService();
  Camunda7TaskEventResourceAssembler camunda7TaskEventResourceAssembler =
      new Camunda7TaskEventResourceAssembler();
  Camunda7TaskEventListResourceAssembler camunda7TaskEventListResourceAssembler =
      new Camunda7TaskEventListResourceAssembler();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEvents(@Context UriInfo uriInfo) throws InvalidArgumentException {

    Camunda7TaskEventList camunda7TaskEventList = new Camunda7TaskEventList();

    MultivaluedMap<String, String> filterParams = uriInfo.getQueryParameters();

    List<Camunda7TaskEvent> camunda7TaskEvents = camundaTaskEventService.getEvents(filterParams);

    camunda7TaskEventList.setCamundaTaskEvents(camunda7TaskEvents);

    Camunda7TaskEventListResource camunda7TaskEventListResource =
        camunda7TaskEventListResourceAssembler.toResource(camunda7TaskEventList);

    return Response.status(200).entity(camunda7TaskEventListResource).build();
  }

  @Path(Mapping.URL_EVENT)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEvent(@PathParam("eventId") final int eventId)
      throws Camunda7TaskEventNotFoundException {

    Camunda7TaskEvent camunda7TaskEvent = camundaTaskEventService.getEvent(eventId);

    Camunda7TaskEventResource camunda7TaskEventResource =
        camunda7TaskEventResourceAssembler.toResource(camunda7TaskEvent);

    return Response.status(200).entity(camunda7TaskEventResource).build();
  }

  @Path(Mapping.URL_DELETE_EVENTS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteEvents(String idsAsString) {

    camundaTaskEventService.deleteEvents(idsAsString);

    return Response.status(204).build();
  }

  @Path(Mapping.URL_DECREASE_REMAINING_RETRIES)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response decreaseRemainingRetriesAndLogError(
      String eventIdOfTaskFailedToStartAndErrorLog) {

    camundaTaskEventService.decreaseRemainingRetriesAndLogError(
        eventIdOfTaskFailedToStartAndErrorLog);

    return Response.status(204).build();
  }

  @Path(Mapping.URL_UNLOCK_EVENT)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response unlockEvent(@PathParam("eventId") final int eventId) {

    camundaTaskEventService.unlockEventForId(eventId);

    return Response.status(204).build();
  }

  @Path(Mapping.URL_EVENT)
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response setRemainingRetries(@PathParam("eventId") int eventId, String body)
      throws InvalidArgumentException, Camunda7TaskEventNotFoundException, JsonProcessingException {

    Map<String, Integer> patchMap =
        OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Integer>>() {});

    if (patchMap == null || !patchMap.containsKey(REMAINING_RETRIES)) {
      throw new InvalidArgumentException(
          "Please provide a valid json body in the format {\"remainingRetries\":3}");
    }

    int retriesToSet = patchMap.get(REMAINING_RETRIES);

    Camunda7TaskEvent camunda7TaskEvent =
        camundaTaskEventService.setRemainingRetries(eventId, retriesToSet);

    Camunda7TaskEventResource camunda7TaskEventResource =
        camunda7TaskEventResourceAssembler.toResource(camunda7TaskEvent);

    return Response.status(200).entity(camunda7TaskEventResource).build();
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response setRemainingRetriesForMultipleEvents(
      @QueryParam("retries") final String retries, String body)
      throws InvalidArgumentException, JsonProcessingException {

    if (retries == null || retries.isEmpty()) {
      throw new InvalidArgumentException("Please provide a valid \"retries\" query parameter");
    }

    Map<String, Integer> patchMap =
        OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Integer>>() {});

    if (patchMap == null || !patchMap.containsKey(REMAINING_RETRIES)) {
      throw new InvalidArgumentException(
          "Please provide a valid json body in the format {\"remainingRetries\":3}");
    }

    int retriesToSet = patchMap.get(REMAINING_RETRIES);

    int remainingRetries = Integer.parseInt(retries);

    List<Camunda7TaskEvent> camunda7TaskEvents =
        camundaTaskEventService.setRemainingRetriesForMultipleEvents(
            remainingRetries, retriesToSet);

    Camunda7TaskEventList camunda7TaskEventList = new Camunda7TaskEventList();
    camunda7TaskEventList.setCamundaTaskEvents(camunda7TaskEvents);

    Camunda7TaskEventListResource camunda7TaskEventListResource =
        camunda7TaskEventListResourceAssembler.toResource(camunda7TaskEventList);

    return Response.status(200).entity(camunda7TaskEventListResource).build();
  }

  @Path(Mapping.URL_EVENT)
  @DELETE
  public Response deleteFailedEvent(@PathParam("eventId") int eventId) {

    camundaTaskEventService.deleteFailedEvent(eventId);

    return Response.status(204).build();
  }

  @Path(Mapping.DELETE_FAILED_EVENTS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteAllFailedEvents() {

    camundaTaskEventService.deleteAllFailedEvents();

    return Response.status(204).build();
  }

  @GET
  @Path(Mapping.URL_COUNT_FAILED_EVENTS)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventsCount(@QueryParam("retries") int remainingRetries) {

    String failedEventsCount = camundaTaskEventService.getEventsCount(remainingRetries);

    return Response.status(200).entity(failedEventsCount).build();
  }
}
