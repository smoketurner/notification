package com.smoketurner.notification.application.filter;

import java.io.IOException;
import java.text.DecimalFormat;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

/**
 * This class adds an "X-Runtime" HTTP response header that includes the time taken to execute the
 * request, in seconds.
 */
@PreMatching
public class RuntimeFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String RUNTIME_HEADER = "X-Runtime";
  private static final String RUNTIME_PROPERTY = "com.smoketurner.notification.runtime";

  @Override
  public void filter(final ContainerRequestContext request) throws IOException {
    request.setProperty(RUNTIME_PROPERTY, System.nanoTime());
  }

  @Override
  public void filter(final ContainerRequestContext request, final ContainerResponseContext response)
      throws IOException {
    final Long startTime = (Long) request.getProperty(RUNTIME_PROPERTY);
    if (startTime != null) {
      final double delta = (System.nanoTime() - startTime) / 1000000000.0;
      response.getHeaders().add(RUNTIME_HEADER, new DecimalFormat("#.#####").format(delta));
    }
  }
}
