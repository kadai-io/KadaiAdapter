package io.kadai.adapter.camunda.outbox.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;

@Provider
public class CsrfTokenIssuanceFilter implements ContainerResponseFilter {
  private static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";

  @Autowired CsrfTokenService csrfTokenService;

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (requestContext.getCookies().containsKey(CSRF_TOKEN_COOKIE_NAME)) {
      return;
    }

    String randomToken = csrfTokenService.createRandomToken();
    NewCookie tokenCookie =
        new NewCookie(CSRF_TOKEN_COOKIE_NAME, randomToken, "/", null, null, -1, false, false);
    responseContext.getHeaders().add("Set-Cookie", tokenCookie);
  }
}
