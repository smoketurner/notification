package com.smoketurner.notification.application.store;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.ge.snowizard.core.IdWorker;
import com.ge.snowizard.exceptions.InvalidSystemClock;
import com.google.common.base.Optional;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.NotificationListAddition;
import com.smoketurner.notification.application.riak.NotificationListDeletion;
import com.smoketurner.notification.application.riak.NotificationListObject;

public class NotificationStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationStore.class);
    private static final String BUCKET_NAME = "notifications";
    private final RiakClient client;
    private final IdWorker snowizard;

    /**
     * Constructor
     *
     * @param client
     *            Riak client
     * @param snowizard
     *            ID Generator
     */
    public NotificationStore(@Nonnull final RiakClient client,
            @Nonnull final IdWorker snowizard) {
        this.client = checkNotNull(client);
        this.snowizard = checkNotNull(snowizard);
    }

    public Optional<List<Notification>> fetch(@Nonnull final String username)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");

        final Location location = new Location(new Namespace(BUCKET_NAME),
                username);

        final NotificationListObject list;
        final FetchValue fv = new FetchValue.Builder(location).build();
        try {
            final FetchValue.Response response = client.execute(fv);
            list = response.getValue(NotificationListObject.class);
        } catch (UnresolvedConflictException e) {
            LOGGER.error("Unable to resolve Riak siblings", e);
            throw new NotificationStoreException(e);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to fetch key (" + location + ") from Riak", e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Fetch request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }

        if (list == null) {
            return Optional.absent();
        }
        return Optional.of(list.getNotificationList());
    }

    public Notification store(@Nonnull final String username,
            @Nonnull final Notification notification)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");
        checkNotNull(notification);

        final long id;
        try {
            id = snowizard.nextId();
        } catch (InvalidSystemClock e) {
            LOGGER.error("Clock is moving backward to generate IDs", e);
            throw new NotificationStoreException(e);
        }

        final Notification updatedNotification = Notification.newBuilder()
                .fromNotification(notification).withId(id)
                .withCreatedAt(DateTime.now(DateTimeZone.UTC)).build();

        final NotificationListAddition update = new NotificationListAddition(
                updatedNotification);

        final Location location = new Location(new Namespace(BUCKET_NAME),
                username);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key (" + location + ") from Riak", e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
        return updatedNotification;
    }

    public void removeAll(@Nonnull final String username)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");

        final Location location = new Location(new Namespace(BUCKET_NAME),
                username);
        final DeleteValue deleteValue = new DeleteValue.Builder(location)
                .build();

        try {
            client.execute(deleteValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to delete key (" + location + ") from Riak", e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Delete request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
    }

    public void remove(@Nonnull final String username,
            @Nonnull final Collection<Long> ids)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");
        checkNotNull(ids);

        final Location location = new Location(new Namespace(BUCKET_NAME),
                username);
        final NotificationListDeletion delete = new NotificationListDeletion(
                ids);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(delete)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key (" + location + ") from Riak", e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
    }
}
