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
        this.environment = Objects.requireNonNull(environment,
                "environment == null");
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
        return build(configuration, client);
    }

    /**
     * Build a new {@link NotificationClient}
     * 
     * @param configuration
     *            Configuration to use for the client
     * @param client
     *            Jersey Client to use
     * @return new NotificationClient
     */
    public NotificationClient build(
            @Nonnull final NotificationClientConfiguration configuration,
            @Nonnull final Client client) {
        return new NotificationClient(environment.metrics(), client,
                configuration.getUri());
    }
}
