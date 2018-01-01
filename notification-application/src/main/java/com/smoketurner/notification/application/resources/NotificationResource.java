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
package com.smoketurner.notification.application.resources;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.server.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.LongSetParam;
import com.smoketurner.notification.application.core.RangeHeader;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationException;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/v1/notifications")
@Api(value = "notifications")
public class NotificationResource {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationResource.class);
    private static final String ACCEPT_RANGES_HEADER = "Accept-Ranges";
    private static final String CONTENT_RANGE_HEADER = "Content-Range";
    private static final String NEXT_RANGE_HEADER = "Next-Range";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 1000;
    private static final String RANGE_NAME = "id";
    private final NotificationStore store;

    /**
     * Constructor
     *
     * @param store
     *            Notification data store
     */
    public NotificationResource(@Nonnull final NotificationStore store) {
        this.store = Objects.requireNonNull(store);
    }

    @GET
    @JSONP
    @Timed
    @Path("/{username}")
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    @ApiOperation(value = "Fetch Notifications", notes = "Return notifications for the given username", responseContainer = "List", response = Notification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Unable to fetch notifications", response = ErrorMessage.class),
            @ApiResponse(code = 404, message = "Notifications not found", response = ErrorMessage.class) })
    public Response fetch(
            @ApiParam(value = "range header", required = false) @HeaderParam("Range") final String rangeHeader,
            @ApiParam(value = "username", required = true) @PathParam("username") final String username) {

        final Optional<UserNotifications> list;
        try {
            list = store.fetch(username);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to fetch notifications", e);
        }

        if (!list.isPresent()) {
            throw new NotificationException(Response.Status.NOT_FOUND,
                    "Notifications not found");
        }

        final ImmutableSortedSet<Notification> notifications = list.get()
                .getNotifications();
        final int total = notifications.size();

        // if there are no notifications, just return an empty list
        if (total < 1) {
            return Response.ok(notifications)
                    .header(ACCEPT_RANGES_HEADER, RANGE_NAME).build();
        }

        // The newest notification is always the first notification in the list
        // and is used to set the Last-Modified response header below.
        final Notification newest = notifications.first();
        final Notification oldest = notifications.last();

        Notification from = newest;
        boolean fromInclusive = true;
        Notification to;
        boolean toInclusive = true;

        int limit = DEFAULT_LIMIT;
        final ResponseBuilder builder;

        // If no Range header is present on the request, return a 200 response
        if (rangeHeader == null) {
            builder = Response.ok();
            try {
                to = Iterables.getLast(
                        store.skip(notifications, from.getId(0L), true, limit));
            } catch (NoSuchElementException e) {
                LOGGER.debug("List of notifications is empty, using oldest", e);
                to = oldest;
            }
        } else {
            // If a Range header is present, return a 206 response
            builder = Response.status(Response.Status.PARTIAL_CONTENT);
            final RangeHeader range = RangeHeader.parse(rangeHeader);
            limit = range.getMax().orElse(DEFAULT_LIMIT);
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
            try {
                if (range.getFromId().isPresent()) {
                    from = notifications.floor(Notification.builder()
                            .withId(range.getFromId().get()).build());
                    if (from == null) {
                        from = newest;
                    }
                    fromInclusive = range.getFromInclusive().orElse(true);

                    to = Iterables.getLast(store.skip(notifications,
                            from.getId(0L), fromInclusive, limit));
                } else {
                    to = Iterables.getLast(store.skip(notifications,
                            from.getId(0L), true, limit));
                }
            } catch (NoSuchElementException e) {
                LOGGER.debug("List of notifications is empty, using oldest", e);
                to = oldest;
            }
        }

        // Add the Accept-Ranges response header
        builder.header(ACCEPT_RANGES_HEADER, RANGE_NAME);

        // Add the Last-Modified response header
        builder.lastModified(Date.from(newest.getCreatedAt().toInstant()));

        final ImmutableSortedSet<Notification> subSet = notifications
                .subSet(from, fromInclusive, to, toInclusive);
        if (!subSet.isEmpty()) {
            final long firstId = subSet.first().getId(0L);
            final long lastId = subSet.last().getId(0L);

            // Add the Content-Range and Next-Range response headers
            builder.header(CONTENT_RANGE_HEADER,
                    String.format("%s %d..%d", RANGE_NAME, firstId, lastId));
            if (subSet.last().compareTo(oldest) < 0) {
                builder.header(NEXT_RANGE_HEADER, String
                        .format("%s ]%d..; max=%d", RANGE_NAME, lastId, limit));
            }
        }

        return builder.entity(subSet).build();
    }

    @POST
    @Timed
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Store Notification", notes = "Add a new notification", response = Notification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Unable to store notification", response = ErrorMessage.class) })
    public Response add(
            @ApiParam(value = "username", required = true) @PathParam("username") final String username,
            @ApiParam(value = "notification", required = true) @NotNull @Valid final Notification notification) {

        final Notification storedNotification;
        try {
            storedNotification = store.store(username, notification);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to store notification", e);
        }

        return Response
                .created(UriBuilder.fromResource(NotificationResource.class)
                        .path("{username}").build(username))
                .entity(storedNotification).build();
    }

    @DELETE
    @Timed
    @Path("/{username}")
    @ApiOperation(value = "Delete Notifications", notes = "Delete individual or all notifications")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Unable to delete notifications", response = ErrorMessage.class) })
    public Response delete(
            @ApiParam(value = "username", required = true) @PathParam("username") final String username,
            @ApiParam(value = "ids", required = false) @QueryParam("ids") final LongSetParam idsParam) {

        if (idsParam != null) {
            store.remove(username, idsParam.get());
        } else {
            store.removeAll(username);
        }

        return Response.noContent().build();
    }
}
