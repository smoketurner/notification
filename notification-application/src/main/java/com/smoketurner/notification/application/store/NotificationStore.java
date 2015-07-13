package com.smoketurner.notification.application.store;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.nio.charset.StandardCharsets;
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
import com.basho.riak.client.api.commands.buckets.StoreBucketProperties;
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
import com.smoketurner.notification.application.managed.NotificationStoreManager;
import com.smoketurner.notification.application.riak.CursorUpdate;
import com.smoketurner.notification.application.riak.NotificationListAddition;
import com.smoketurner.notification.application.riak.NotificationListDeletion;
import com.smoketurner.notification.application.riak.NotificationListObject;

public class NotificationStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationStore.class);
    private static final String CURSOR_NAME = "notifications";
    private final Namespace notificationsNamespace = new Namespace(
            "notifications", StandardCharsets.UTF_8);
    private final Namespace cursorsNamespace = new Namespace("cursors",
            StandardCharsets.UTF_8);
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

    /**
     * Ensure both buckets allow siblings. This is called by
     * {@link NotificationStoreManager} upon start up.
     */
    public void initialize() {
        setAllowMultiForBucket(notificationsNamespace, true);
        setAllowMultiForBucket(cursorsNamespace, true);
    }

    /**
     * Internal method to set the allowMulti setting on a given namespace
     *
     * @param namespace
     *            Riak namespace to set
     * @param allowMulti
     *            desired allow multi setting
     */
    public void setAllowMultiForBucket(@Nonnull final Namespace namespace,
            final boolean allowMulti) {
        checkNotNull(namespace);

        LOGGER.debug("Setting allow_multi={} for namespace={}", allowMulti,
                namespace);
        final StoreBucketProperties storeBucketProperties = new StoreBucketProperties.Builder(
                namespace).withAllowMulti(allowMulti).build();

        try {
            client.execute(storeBucketProperties);
        } catch (InterruptedException e) {
            LOGGER.warn(String.format(
                    "Unable to set allow_multi=%s for namespace=%s",
                    allowMulti, namespace), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error(String.format(
                    "Unable to set allow_multi=%s for namespace=%s",
                    allowMulti, namespace), e);
        }
    }

    /**
     * Fetch a list of notifications for a given user
     *
     * @param username
     *            User to fetch notifications for
     * @return Optional list of notifications or absent
     * @throws NotificationStoreException
     *             if unable to fetch the notifications
     */
    public Optional<List<Notification>> fetch(@Nonnull final String username)
            throws NotificationStoreException {

        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");

        final Location location = new Location(notificationsNamespace, username);

        LOGGER.debug("Fetching key: {}", location);

        final NotificationListObject list;
        final FetchValue fv = new FetchValue.Builder(location).build();
        try {
            final FetchValue.Response response = client.execute(fv);
            list = response.getValue(NotificationListObject.class);
        } catch (UnresolvedConflictException e) {
            LOGGER.error(
                    "Unable to resolve Riak siblings for key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to fetch key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted fetching key: " + location, e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }

        if (list == null) {
            return Optional.absent();
        }
        return Optional.of(list.getNotificationList());
    }

    /**
     * Store a new notification for a user
     *
     * @param username
     *            User to store the notification
     * @param notification
     *            Notification to store
     * @return the stored notification
     * @throws NotificationStoreException
     *             if unable to store the notification
     */
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
                .fromNotification(notification).withId(id).withCreatedAt(now())
                .build();

        final NotificationListAddition update = new NotificationListAddition(
                updatedNotification);

        final Location location = new Location(notificationsNamespace, username);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);
        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
        return updatedNotification;
    }

    /**
     * Delete all of the notifications for a given user
     * 
     * @param username
     *            User to delete all the notifications
     * @throws NotificationStoreException
     *             if unable to delete all the notifications
     */
    public void removeAll(@Nonnull final String username)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");

        final Location location = new Location(notificationsNamespace, username);
        final DeleteValue deleteValue = new DeleteValue.Builder(location)
                .build();

        LOGGER.debug("Deleting key: {}", location);
        try {
            client.execute(deleteValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to delete key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Delete request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
    }

    /**
     * Remove individual notifications for a given user
     *
     * @param username
     *            User to remove notifications from
     * @param ids
     *            Notification IDs to remove
     * @throws NotificationStoreException
     *             if unable to remove the notifications
     */
    public void remove(@Nonnull final String username,
            @Nonnull final Collection<Long> ids)
            throws NotificationStoreException {
        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");
        checkNotNull(ids);

        // if nothing to remove, return early
        if (ids.isEmpty()) {
            return;
        }

        final Location location = new Location(notificationsNamespace, username);
        final NotificationListDeletion delete = new NotificationListDeletion(
                ids);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(delete)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);
        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
    }

    /**
     * Update a given cursor with the specified value.
     *
     * @param username
     *            Username to update the cursor for
     * @param value
     *            Value to set
     * @throws NotificationStoreException
     *             if unable to set the cursor
     */
    public void updateCursor(@Nonnull final String username, final long value)
            throws NotificationStoreException {

        checkNotNull(username);
        checkArgument(!username.isEmpty(), "username cannot be empty");

        final String key = String.format("%s-%s", username, CURSOR_NAME);
        final CursorUpdate update = new CursorUpdate(key, value);

        final Location location = new Location(cursorsNamespace, key);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);
        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        }
    }

    public static DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }
}
