package com.smoketurner.notification.application.resources;

import io.dropwizard.jersey.caching.CacheControl;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.validation.Valid;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.LongSetParam;
import com.smoketurner.notification.application.core.RangeHeader;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationException;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;

@Path("/v1/notifications")
public class NotificationResource {

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
        this.store = Preconditions.checkNotNull(store);
    }

    @GET
    @Timed
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    public Response fetch(@HeaderParam("Range") final String rangeHeader,
            @PathParam("username") final String username) {
        final Optional<UserNotifications> list;
        try {
            list = store.fetch(username);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to fetch notifications");
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

        final Notification mostRecent = notifications.first();
        long startId = mostRecent.getId(0L);

        int limit = DEFAULT_LIMIT;
        final ResponseBuilder builder;
        if (rangeHeader == null) {
            builder = Response.ok();
        } else {
            builder = Response.status(Response.Status.PARTIAL_CONTENT);
            final RangeHeader range = RangeHeader.parse(rangeHeader);
            if (range.getId().isPresent()) {
                startId = range.getId().get();
            }
            limit = range.getMax().or(DEFAULT_LIMIT);
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
        }

        // Add the Last-Modified response header
        builder.lastModified(new Date(mostRecent.getCreatedAt()
                .or(DateTime.now(DateTimeZone.UTC)).getMillis()));

        final ImmutableSortedSet<Notification> subSet = ImmutableSortedSet
                .copyOf(store.skip(notifications, startId, limit));

        final long firstId = subSet.first().getId(0L);
        final long lastId = subSet.last().getId(0L);

        // Add the Accept-Ranges, Content-Range and Next-Range response headers
        builder.header(ACCEPT_RANGES_HEADER, RANGE_NAME);
        builder.header(CONTENT_RANGE_HEADER,
                String.format("%s %d..%d", RANGE_NAME, firstId, lastId));
        if (firstId > lastId) {
            builder.header(NEXT_RANGE_HEADER,
                    String.format("%s %d; max=%d", RANGE_NAME, lastId, limit));
        }

        return builder.entity(subSet).build();
    }

    @POST
    @Timed
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@PathParam("username") final String username,
            @Valid final Notification notification) {

        final Notification storedNotification;
        try {
            storedNotification = store.store(username, notification);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to store notification");
        }

        return Response
                .created(
                        UriBuilder.fromResource(NotificationResource.class)
                                .path("{username}").build(username))
                .entity(storedNotification).build();
    }

    @DELETE
    @Timed
    @Path("/{username}")
    public Response delete(@PathParam("username") final String username,
            @QueryParam("ids") final Optional<LongSetParam> idsParam) {

        if (idsParam.isPresent()) {
            try {
                store.remove(username, idsParam.get().get());
            } catch (NotificationStoreException e) {
                throw new NotificationException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "Unable to delete notifications");
            }
        } else {
            try {
                store.removeAll(username);
            } catch (NotificationStoreException e) {
                throw new NotificationException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "Unable to delete all notifications");
            }
        }

        return Response.noContent().build();
    }
}
