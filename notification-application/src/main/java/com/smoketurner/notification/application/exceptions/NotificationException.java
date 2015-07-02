package com.smoketurner.notification.application.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class NotificationException extends WebApplicationException {

    private static final long serialVersionUID = 4359514441284675684L;
    private final int code;
    private final Response.Status status;
    private final String message;

    /**
     * Constructor
     *
     * @param code
     *            Status code to return
     * @param message
     *            Error message to return
     */
    public NotificationException(final int code, final String message) {
        super(code);
        this.code = code;
        this.status = Response.Status.fromStatusCode(code);
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param status
     *            Status code to return
     * @param message
     *            Error message to return
     */
    public NotificationException(final Response.Status status,
            final String message) {
        super(status);
        this.code = status.getStatusCode();
        this.status = status;
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param status
     *            Status code to return
     * @param message
     *            Error message to return
     * @param cause
     *            Throwable which caused the exception
     */
    public NotificationException(final Response.Status status,
            final String message, final Throwable cause) {
        super(cause, status);
        this.code = status.getStatusCode();
        this.status = status;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public Response.Status getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}