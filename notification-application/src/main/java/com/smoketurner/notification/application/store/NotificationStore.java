package com.smoketurner.notification.application.store;

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
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ge.snowizard.core.IdWorker;
import com.ge.snowizard.exceptions.InvalidSystemClock;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.NotificationListAddition;
import com.smoketurner.notification.application.riak.NotificationListDeletion;
import com.smoketurner.notification.application.riak.NotificationListObject;

public class NotificationStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationStore.class);
    private static final String CURSOR_NAME = "notifications";
    private static final Namespace NAMESPACE = new Namespace("notifications",
            StandardCharsets.UTF_8);
    private final RiakClient client;
    private final IdWorker snowizard;
    private final CursorStore cursors;

    // timers
    private final Timer fetchTimer;
    private final Timer storeTimer;
    private final Timer deleteTimer;

    /**
     * Constructor
     *
     * @param registry
     *            Metric registry
     * @param client
     *            Riak client
     * @param snowizard
     *            ID Generator
     * @param cursors
     *            Cursor data store
     */
    public NotificationStore(@Nonnull final MetricRegistry registry,
            @Nonnull final RiakClient client,
            @Nonnull final IdWorker snowizard,
            @Nonnull final CursorStore cursors) {
        Preconditions.checkNotNull(registry);
        this.fetchTimer = registry.timer(MetricRegistry.name(
                NotificationStore.class, "fetch"));
        this.storeTimer = registry.timer(MetricRegistry.name(
                NotificationStore.class, "store"));
        this.deleteTimer = registry.timer(MetricRegistry.name(
                NotificationStore.class, "delete"));

        this.client = Preconditions.checkNotNull(client);
        this.snowizard = Preconditions.checkNotNull(snowizard);
        this.cursors = Preconditions.checkNotNull(cursors);
    }

    /**
     * Internal method to set the allow_multi to true
     */
    public void initialize() {
        final boolean allowMulti = true;
        LOGGER.debug("Setting allow_multi={} for namespace={}", allowMulti,
                NAMESPACE);
        final StoreBucketProperties storeBucketProperties = new StoreBucketProperties.Builder(
                NAMESPACE).withAllowMulti(allowMulti).build();

        try {
            client.execute(storeBucketProperties);
        } catch (InterruptedException e) {
            LOGGER.warn(String.format(
                    "Unable to set allow_multi=%s for namespace=%s",
                    allowMulti, NAMESPACE), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error(String.format(
                    "Unable to set allow_multi=%s for namespace=%s",
                    allowMulti, NAMESPACE), e);
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

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");

        final Location location = new Location(NAMESPACE, username);

        LOGGER.debug("Fetching key: {}", location);

        final NotificationListObject list;
        final FetchValue fv = new FetchValue.Builder(location).build();
        final Timer.Context context = fetchTimer.time();
        try {
            final FetchValue.Response response = client.execute(fv);
            list = response.getValue(NotificationListObject.class);
        } catch (UnresolvedConflictException e) {
            LOGGER.error("Unable to resolve siblings for key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to fetch key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted fetching key: " + location, e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        } finally {
            context.stop();
        }

        if (list == null) {
            return Optional.absent();
        }

        final List<Notification> notifications = setUnseenState(username,
                list.getNotificationList());
        return Optional.of(notifications);
    }

    /**
     * Sets the unseen state on all of the notifications based any previously
     * viewed notifications.
     * 
     * @param username
     *            Username of the notifications
     * @param notifications
     *            Original notifications list
     * @return a notifications list beginning with the unseen notifications,
     *         then the seen the notifications
     * @throws NotificationStoreException
     *             if unable to update the cursor
     */
    public List<Notification> setUnseenState(@Nonnull final String username,
            final List<Notification> notifications)
            throws NotificationStoreException {

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Preconditions.checkNotNull(notifications);

        // if there are no notifications, just return
        if (notifications.isEmpty()) {
            return notifications;
        }

        // get the ID of the most recent notification
        final long mostRecentId = notifications.get(0).getId().get();
        LOGGER.debug("Most recent notification ID: {}", mostRecentId);

        final Optional<Long> cursor = cursors.fetch(username, CURSOR_NAME);
        final long lastSeenId = cursor.or(0L);
        if (!cursor.isPresent()) {
            LOGGER.debug("User ({}) has no cursor", username);

            // if there are no seen notifications, update to the cursor to the
            // most recent notification
            cursors.store(username, CURSOR_NAME, mostRecentId);

            // set all of the notifications to unseen=true
            return ImmutableList.<Notification> builder()
                    .addAll(setUnseenState(notifications, true)).build();
        }

        LOGGER.debug("Current cursor ID: {}", lastSeenId);

        // if the latest seen notification ID is less than the most recent
        // notification ID, then update the cursor to the most recent
        // notification ID.
        if (lastSeenId < mostRecentId) {
            LOGGER.debug("Updating cursor ID to {}", mostRecentId);
            cursors.store(username, CURSOR_NAME, mostRecentId);
        }

        // get the position of the last seen notification ID
        final int lastSeenOffset = findNotification(notifications, lastSeenId);
        LOGGER.debug("lastSeenId: {}, lastSeenOffset: {}", lastSeenId,
                lastSeenOffset);
        // if the notification is not found in the list, assume all of the
        // notifications are unseen.
        if (lastSeenOffset == -1) {
            LOGGER.debug("Last seen ID {} not found, assuming all unseen",
                    lastSeenId);
            return ImmutableList.<Notification> builder()
                    .addAll(setUnseenState(notifications, true)).build();
        }

        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();

        // unseen=true
        final List<Notification> unseen = notifications.subList(0,
                lastSeenOffset);
        builder.addAll(setUnseenState(unseen, true));

        // unseen=false
        final List<Notification> seen = notifications.subList(lastSeenOffset,
                notifications.size());
        builder.addAll(setUnseenState(seen, false));

        return builder.build();
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

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Preconditions.checkNotNull(notification);

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

        final Location location = new Location(NAMESPACE, username);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);

        final Timer.Context context = storeTimer.time();
        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        } finally {
            context.stop();
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

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");

        final Location location = new Location(NAMESPACE, username);
        final DeleteValue deleteValue = new DeleteValue.Builder(location)
                .build();

        LOGGER.debug("Deleting key: {}", location);
        final Timer.Context context = deleteTimer.time();
        try {
            client.execute(deleteValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to delete key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Delete request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        } finally {
            context.stop();
        }

        cursors.delete(username, CURSOR_NAME);
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

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Preconditions.checkNotNull(ids);

        // if nothing to remove, return early
        if (ids.isEmpty()) {
            return;
        }

        final Location location = new Location(NAMESPACE, username);
        final NotificationListDeletion delete = new NotificationListDeletion(
                ids);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(delete)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);
        final Timer.Context context = deleteTimer.time();
        try {
            client.execute(updateValue);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to update key: " + location, e);
            throw new NotificationStoreException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Update request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new NotificationStoreException(e);
        } finally {
            context.stop();
        }
    }

    /**
     * Set the unseen state on multiple notifications
     *
     * @param notifications
     *            notifications to update
     * @param unseen
     *            whether the notifications have been seen or not
     * @return the updated notifications
     */
    public Iterable<Notification> setUnseenState(
            final Iterable<Notification> notifications, final boolean unseen) {
        return Iterables.transform(notifications,
                new Function<Notification, Notification>() {
                    @Override
                    public Notification apply(final Notification notification) {
                        return Notification.newBuilder()
                                .fromNotification(notification)
                                .withUnseen(unseen).build();
                    }
                });
    }

    /**
     * Find the offset of a given notification if it exists
     *
     * @param notifications
     *            Notifications to search through
     * @param id
     *            Notification ID to find
     * @return the position of the notification in notifications
     */
    public int findNotification(final Iterable<Notification> notifications,
            final long id) {
        return Iterables.indexOf(notifications, new Predicate<Notification>() {
            @Override
            public boolean apply(final Notification notification) {
                // first check that the ID matches
                final Optional<Long> notificationId = notification.getId();
                if (!notificationId.isPresent()) {
                    return false;
                } else if (notificationId.get() == id) {
                    return true;
                }

                // then check to see if the notification is included in any
                // rolled up notifications
                final Collection<Notification> childNotifications = notification
                        .getNotifications().or(
                                ImmutableList.<Notification> of());
                if (childNotifications.isEmpty()) {
                    return false;
                }
                return findNotification(childNotifications, id) != -1;
            }
        });
    }

    /**
     * Return the current date time (overridden in tests)
     *
     * @return the current date time
     */
    public DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }
}
