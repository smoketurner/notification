package com.smoketurner.notification.client;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;

public class NotificationClientBuilder {
    private final Environment environment;

    /**
     * Constructor
     *
     * @param environment
     *            Environment
     */
    public NotificationClientBuilder(@Nonnull final Environment environment) {
        this.environment = Objects.requireNonNull(environment);
    }

    /**
     * Build a new {@link NotificationClient}
     * 
     * @param configuration
     *            Configuration to use for the client
     * @return new NotificationClient
     */
    public NotificationClient build(
            @Nonnull final NotificationClientConfiguration configuration) {
        final Client client = new JerseyClientBuilder(environment)
                .using(configuration).build("notification");

        return new NotificationClient(environment.metrics(), client,
                configuration.getUri());
    }
}
