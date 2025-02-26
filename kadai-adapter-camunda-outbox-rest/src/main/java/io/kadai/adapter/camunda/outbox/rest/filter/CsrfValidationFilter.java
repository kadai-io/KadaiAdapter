package io.kadai.adapter.camunda.outbox.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

@Provider
public class CsrfValidationFilter implements ContainerRequestFilter {

  private static final String CSRF_TOKEN_HEADER_NAME = "X-XSRF-TOKEN";
  private static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
  private static final Response CSRF_TOKEN_COOKIE_AND_HEADER_NOT_MATCH =
      Response.status(Response.Status.FORBIDDEN)
          .entity("CSRF token cookie and header does not match")
          .build();
  private static final Response CSRF_TOKEN_COOKIE_INVALID =
      Response.status(Response.Status.FORBIDDEN).entity("CSRF token is invalid.").build();
  private static final Response CSRF_TOKEN_HEADER_NULL =
      Response.status(Response.Status.FORBIDDEN).entity("Missing X-XSRF-TOKEN Header.").build();
  private static final Response CSRF_TOKEN_HEADER_WRONG_SIZE =
      Response.status(Response.Status.FORBIDDEN)
          .entity("There should only be one X-XSRF-TOKEN Header")
          .build();
  private static final Response XSRF_TOKEN_COOKIE_NULL =
      Response.status(Response.Status.FORBIDDEN).entity("Missing XSRF-TOKEN Cookie").build();
  private static final Response XSRF_TOKEN_COOKIE_WRONG_SIZE =
      Response.status(Response.Status.FORBIDDEN)
          .entity("There should only be one XSRF-TOKEN Cookie")
          .build();

  @Autowired CsrfTokenService csrfTokenService;

  private static final String CSRF_TOKEN_ISSUE_ENDPOINT = "/events/csrf";

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    if (requestContext.getUriInfo().getPath().equals(CSRF_TOKEN_ISSUE_ENDPOINT)) {
      return;
    }

    String cookieToken;
    String cookieHeader = requestContext.getHeaderString(HttpHeaders.COOKIE);
    if (cookieHeader != null) {
      List<String> xsrfTokens =
          Arrays.stream(cookieHeader.split(";"))
              .map(String::trim)
              .filter(cookie -> cookie.startsWith(CSRF_TOKEN_COOKIE_NAME + "="))
              .collect(Collectors.toList());

      if (xsrfTokens.size() > 1) {
        requestContext.abortWith(XSRF_TOKEN_COOKIE_WRONG_SIZE);
        return;
      } else if (xsrfTokens.isEmpty()) {
        requestContext.abortWith(XSRF_TOKEN_COOKIE_NULL);
        return;
      } else {
        cookieToken = xsrfTokens.get(0).split("=")[1].replace("\"", "");
      }
    } else {
      requestContext.abortWith(XSRF_TOKEN_COOKIE_NULL);
      return;
    }

    List<String> csrfTokenHeader = requestContext.getHeaders().get(CSRF_TOKEN_HEADER_NAME);

    String headerToken = null;
    if (csrfTokenHeader == null) {
      requestContext.abortWith(CSRF_TOKEN_HEADER_NULL);
      return;
    } else {
      if (csrfTokenHeader.size() == 1) {
        headerToken = csrfTokenHeader.get(0).replace("\"", "");
      } else {
        requestContext.abortWith(CSRF_TOKEN_HEADER_WRONG_SIZE);
        return;
      }
    }

    if (!headerToken.equals(cookieToken)) {
      requestContext.abortWith(CSRF_TOKEN_COOKIE_AND_HEADER_NOT_MATCH);
    } else if (!csrfTokenService.validateToken(headerToken)) {
      requestContext.abortWith(CSRF_TOKEN_COOKIE_INVALID);
    }
  }
}
