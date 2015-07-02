package com.smoketurner.notification.application.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import jersey.repackaged.com.google.common.base.Splitter;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.exceptions.NotificationException;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;

@Path("/v1/notifications")
public class NotificationResource {

    private final NotificationStore store;

    /**
     * Constructor
     *
     * @param store
     *            Notification data store
     */
    public NotificationResource(@Nonnull final NotificationStore store) {
        this.store = checkNotNull(store);
    }

    @GET
    @Timed
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Notification> fetch(@PathParam("username") final String username) {
        final Optional<List<Notification>> list;
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

        return list.get();
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
            @QueryParam("ids") final Optional<String> idsParam) {

        if (idsParam.isPresent() && !Strings.isNullOrEmpty(idsParam.get())) {
            final Set<Long> ids = Sets.newHashSet();

            for (String id : Splitter.on(',').trimResults()
                    .split(idsParam.get())) {
                try {
                    ids.add(Longs.tryParse(id));
                } catch (NullPointerException ignore) {
                    // ignore nulls
                }
            }

            try {
                store.remove(username, ids);
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
