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

import static com.codahale.metrics.MetricRegistry.name;
import java.io.Closeable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;

public class NotificationClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationClient.class);
    private static final String APPLICATION_JSON = "application/json";
    private final Client client;
    private final Timer fetchTimer;
    private final Timer storeTimer;
    private final Timer deleteTimer;
    private final URI rootUri;

    /**
     * Constructor
     *
     * @param registry
     *            Metric Registry
     * @param client
     *            Jersey Client
     * @param uri
     *            API endpoint
     */
    public NotificationClient(@Nonnull final MetricRegistry registry,
            @Nonnull final Client client, @Nonnull final URI uri) {
        this.client = Objects.requireNonNull(client, "client == null");
        this.fetchTimer = registry
                .timer(name(NotificationClient.class, "fetch"));
        this.storeTimer = registry
                .timer(name(NotificationClient.class, "store"));
        this.deleteTimer = registry
                .timer(name(NotificationClient.class, "delete"));
        this.rootUri = Objects.requireNonNull(uri, "uri == null");
    }

    /**
     * Fetch all notifications for a given username. This method will paginate
     * through all of the available notifications for a user.
     *
     * @param username
     *            User to fetch notifications
     * @return Sorted set of all notifications for the user
     */
    public Optional<ImmutableSortedSet<Notification>> fetch(
            @Nonnull final String username) {

        final URI uri = getTarget(username);
        final ImmutableSortedSet.Builder<Notification> results = ImmutableSortedSet
                .naturalOrder();
        String nextRange = null;
        boolean paginate = true;

        try (Timer.Context context = fetchTimer.time()) {
            while (paginate) {
                LOGGER.info("GET {}", uri);
                final Invocation.Builder builder = client.target(uri)
                        .request(APPLICATION_JSON);
                if (nextRange != null) {
                    builder.header("Range", nextRange);
                }

                final Response response = builder.get();
                nextRange = response.getHeaderString("Next-Range");
                if (nextRange == null) {
                    paginate = false;
                }

                if (response.getStatus() == Response.Status.OK.getStatusCode()
                        || response
                                .getStatus() == Response.Status.PARTIAL_CONTENT
                                        .getStatusCode()) {
                    results.addAll(response
                            .readEntity(new GenericType<List<Notification>>() {
                            }));
                }
                response.close();
            }
            return Optional.of(results.build());

        } catch (Exception e) {
            LOGGER.warn("Unable to fetch notification for {}", username, e);
        }
        return Optional.empty();
    }

    /**
     * Store a new notification for a user
     *
     * @param username
     *            User to add the notification
     * @param notification
     *            Notification to store
     * @return the newly stored notification
     */
    public Optional<Notification> store(@Nonnull final String username,
            @Nonnull final Notification notification) {
        Objects.requireNonNull(notification, "notification == null");
        final URI uri = getTarget(username);
        LOGGER.debug("POST {}", uri);

        try (Timer.Context context = storeTimer.time()) {
            return Optional.of(client.target(uri).request(APPLICATION_JSON)
                    .post(Entity.json(notification), Notification.class));
        } catch (Exception e) {
            LOGGER.warn("Unable to store notification for {}", username, e);
        }
        return Optional.empty();
    }

    /**
     * Delete individual notification IDs for a given user.
     *
     * @param username
     *            User to delete notifications from
     * @param ids
     *            Notification IDs to delete
     */
    public void delete(@Nonnull final String username,
            @Nonnull final Collection<Long> ids) {
        Objects.requireNonNull(ids, "ids == null");
        Preconditions.checkArgument(!ids.isEmpty(), "ids cannot be empty");
        final URI uri = UriBuilder.fromUri(getTarget(username))
                .queryParam("ids", Joiner.on(",").join(ids)).build();
        LOGGER.debug("DELETE {}", uri);

        try (Timer.Context context = deleteTimer.time()) {
            client.target(uri).request().delete();
        } catch (Exception e) {
            LOGGER.warn("Unable to delete notifications for {}", username, e);
        }
    }

    /**
     * Delete all notifications for a given user.
     *
     * @param username
     *            User to delete notifications from
     */
    public void delete(@Nonnull final String username) {
        final URI uri = getTarget(username);
        LOGGER.debug("DELETE {}", uri);

        try (Timer.Context context = deleteTimer.time()) {
            client.target(uri).request().delete();
        } catch (Exception e) {
            LOGGER.warn("Unable to delete notifications for {}", username, e);
        }
    }

    /**
     * Return the ping response
     *
     * @return true if the ping response was successful, otherwise false
     */
    public boolean ping() {
        final URI uri = UriBuilder.fromUri(rootUri).path("/ping").build();
        LOGGER.debug("GET {}", uri);
        final String response = client.target(uri).request().get(String.class);
        return "pong".equals(response);
    }

    /**
     * Return the service version
     *
     * @return service version
     */
    public String version() {
        final URI uri = UriBuilder.fromUri(rootUri).path("/version").build();
        LOGGER.debug("GET {}", uri);
        return client.target(uri).request().get(String.class);
    }

    /**
     * Builds a target URL
     *
     * @param username
     *            Username
     * @return target URL
     */
    private URI getTarget(@Nonnull final String username) {
        Objects.requireNonNull(username, "username == null");
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        return UriBuilder.fromUri(rootUri).path("/v1/notifications/{username}")
                .build(username);
    }

    @Override
    public void close() {
        client.close();
    }
}
