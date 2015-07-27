package com.smoketurner.notification.application.filter;

import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class IdResponseFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IdResponseFilter.class);
    private static final String REQUEST_ID = "Request-Id";

    @Override
    public void filter(final ContainerRequestContext request,
            final ContainerResponseContext response) throws IOException {
        final UUID id = UUID.randomUUID();
        LOGGER.info("method={} path={} request_id={} status={} bytes={}",
                request.getMethod(), request.getUriInfo().getPath(), id,
                response.getStatus(), response.getLength());
        response.getHeaders().add(REQUEST_ID, id);
    }
}
