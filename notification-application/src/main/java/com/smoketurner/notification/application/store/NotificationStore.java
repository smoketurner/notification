/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.store;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.TreeSet;
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
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.NotificationListAddition;
import com.smoketurner.notification.application.riak.NotificationListDeletion;
import com.smoketurner.notification.application.riak.NotificationListObject;

public class NotificationStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationStore.class);
    public static final String CURSOR_NAME = "notifications";
    private static final Namespace NAMESPACE = new Namespace("notifications",
            StandardCharsets.UTF_8);
    private final RiakClient client;
    private final IdWorker snowizard;
    private final CursorStore cursors;

    // timers
    private final Timer fetchTimer;
    private final Timer updateTimer;
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
        this.updateTimer = registry.timer(MetricRegistry.name(
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
    public Optional<UserNotifications> fetch(@Nonnull final String username)
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

        return Optional
                .of(splitNotifications(username, list.getNotifications()));
    }

    /**
     * Sets the unseen state on all of the notifications based any previously
     * viewed notifications.
     * 
     * @param username
     *            Username of the notifications
     * @param notifications
     *            Original notifications list
     * @return the seen and unseen notifications
     * @throws NotificationStoreException
     *             if unable to update the cursor
     */
    public UserNotifications splitNotifications(@Nonnull final String username,
            final TreeSet<Notification> notifications)
            throws NotificationStoreException {

        Preconditions.checkNotNull(username);
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Preconditions.checkNotNull(notifications);

        // if there are no notifications, just return
        if (notifications.isEmpty()) {
            return new UserNotifications();
        }

        // get the ID of the most recent notification (this should never be
        // zero)
        final long newestId = notifications.first().getId(0L);
        LOGGER.debug("Newest notification ID: {}", newestId);

        final Optional<Long> cursor = cursors.fetch(username, CURSOR_NAME);
        final long lastSeenId = cursor.or(0L);
        if (!cursor.isPresent()) {
            LOGGER.debug("User ({}) has no cursor", username);

            // if the user has no cursor, update to the cursor to the
            // newest notification
            cursors.store(username, CURSOR_NAME, newestId);

            // set all of the notifications to unseen=true
            return new UserNotifications(setUnseenState(notifications, true));
        }

        LOGGER.debug("Last seen notification ID: {}", lastSeenId);

        // if the latest seen notification ID is less than the newest
        // notification ID, then update the cursor to the newest
        // notification ID.
        if (lastSeenId < newestId) {
            LOGGER.debug("Updating cursor to {}", newestId);
            cursors.store(username, CURSOR_NAME, newestId);
        }

        // get the parent ID of the last seen notification ID
        final Optional<Notification> lastNotification = tryFind(notifications,
                lastSeenId);
        if (!lastNotification.isPresent()) {
            // if the last notification is not found, set all of the
            // notifications as unseen
            return new UserNotifications(setUnseenState(notifications, true));
        }

        // Set the head of the list as being unseen
        final Iterable<Notification> unseen = setUnseenState(
                notifications.headSet(lastNotification.get()), true);

        // Set the tail of the list as being seen
        final Iterable<Notification> seen = setUnseenState(
                notifications.tailSet(lastNotification.get()), false);

        return new UserNotifications(unseen, seen);
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

        final Notification updatedNotification = Notification.builder()
                .fromNotification(notification).withId(id).withCreatedAt(now())
                .build();

        final NotificationListAddition update = new NotificationListAddition(
                updatedNotification);

        final Location location = new Location(NAMESPACE, username);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key: {}", location);

        final Timer.Context context = updateTimer.time();
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
        final Timer.Context context = updateTimer.time();
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
        Preconditions.checkNotNull(notifications);
        return Iterables.transform(notifications,
                new Function<Notification, Notification>() {
                    @Override
                    public Notification apply(final Notification notification) {
                        return Notification.builder()
                                .fromNotification(notification)
                                .withUnseen(unseen).build();
                    }
                });
    }

    /**
     * Return the parent notification that matches the given ID or is the parent
     * of a child notification.
     *
     * @param notifications
     *            Notifications to search through
     * @param id
     *            Notification ID to find
     * @return the notification
     */
    public Optional<Notification> tryFind(
            final Iterable<Notification> notifications, final long id) {
        Preconditions.checkNotNull(notifications);
        return Iterables.tryFind(notifications, new Predicate<Notification>() {
            @Override
            public boolean apply(final Notification notification) {
                // first check that the ID matches
                final Optional<Long> notificationId = notification.getId();
                if (!notificationId.isPresent()) {
                    return false;
                } else if (notificationId.get() == id) {
                    return true;
                }

                // then check to see if the notification is included in
                // any
                // rolled up notifications
                final Collection<Notification> children = notification
                        .getNotifications().or(
                                ImmutableList.<Notification> of());
                if (children.isEmpty()) {
                    return false;
                }
                return (tryFind(children, id)).isPresent();
            }
        });
    }

    /**
     * Returns the index in notifications that matches the given ID or is the
     * parent of a child notification, or -1 if the notification was not found.
     *
     * @param notifications
     *            Notifications to search through
     * @param id
     *            Notification ID to find
     * @return the position of the notification or -1 if not found
     */
    public int indexOf(final Iterable<Notification> notifications, final long id) {
        Preconditions.checkNotNull(notifications);
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

                // then check to see if the notification is included in
                // any
                // rolled up notifications
                final Collection<Notification> children = notification
                        .getNotifications().or(
                                ImmutableList.<Notification> of());
                if (children.isEmpty()) {
                    return false;
                }
                return indexOf(children, id) != -1;
            }
        });
    }

    /**
     * Returns an iterable that skips forward to a given notification ID then
     * only returns count more notifications. If the given notification ID is
     * not found
     * 
     * @param notifications
     *            Iterable of notifications
     * @param startId
     *            notification ID to start at
     * @param inclusive
     *            Whether to include the startId notification or not
     * @param limitSize
     *            Number of notifications to return
     * @return Iterable containing the subset of the original notifications
     */
    public Iterable<Notification> skip(
            final Iterable<Notification> notifications, final long startId,
            final boolean inclusive, final int limitSize) {
        Preconditions.checkNotNull(notifications);
        final int position = indexOf(notifications, startId);
        if (position == -1) {
            return Iterables.limit(notifications, limitSize);
        }
        if (inclusive) {
            return Iterables.limit(Iterables.skip(notifications, position),
                    limitSize);
        }
        return Iterables.limit(Iterables.skip(notifications, position + 1),
                limitSize);
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
