/**
 * Copyright 2018 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
