package com.smoketurner.notification.client;

import java.net.URI;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import io.dropwizard.client.JerseyClientConfiguration;

public class NotificationClientConfiguration extends JerseyClientConfiguration {

    @NotNull
    private URI uri = URI.create("http://localhost:8080");

    public URI getUri() {
        return uri;
    }

    public void setUri(@Nonnull final URI uri) {
        this.uri = uri;
    }
}
