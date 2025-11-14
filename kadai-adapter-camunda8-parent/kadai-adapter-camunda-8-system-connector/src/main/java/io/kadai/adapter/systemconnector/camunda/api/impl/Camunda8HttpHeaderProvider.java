package io.kadai.adapter.systemconnector.camunda.api.impl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class Camunda8HttpHeaderProvider {

  public HttpHeaders getHttpHeadersForCamunda8TasklistApi() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
