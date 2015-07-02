package com.smoketurner.notification.application.exceptions;

import io.dropwizard.jersey.errors.ErrorMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationExceptionMapper implements
        ExceptionMapper<NotificationException> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationExceptionMapper.class);

    @Override
    public Response toResponse(final NotificationException exception) {
        LOGGER.debug("Error response ({}): {}", exception.getCode(),
                exception.getMessage());

        return Response
                .status(exception.getCode())
                .entity(new ErrorMessage(exception.getCode(), exception
                        .getMessage())).type(MediaType.APPLICATION_JSON)
                .build();
    }
}
