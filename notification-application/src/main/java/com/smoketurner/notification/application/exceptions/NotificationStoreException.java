package com.smoketurner.notification.application.exceptions;

public class NotificationStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotificationStoreException() {
        super();
    }

    public NotificationStoreException(final Throwable cause) {
        super(cause);
    }

    public NotificationStoreException(final String message) {
        super(message);
    }

    public NotificationStoreException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
