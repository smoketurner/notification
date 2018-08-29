/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.store;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.basho.riak.client.api.commands.buckets.StoreBucketProperties;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.core.IdGenerator;
import com.smoketurner.notification.application.core.Rollup;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.NotificationListAddition;
import com.smoketurner.notification.application.riak.NotificationListDeletion;
import com.smoketurner.notification.application.riak.NotificationListObject;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.dropwizard.util.Duration;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationStore.class);
  private static final String CURSOR_NAME = "notifications";
  private static final Namespace NAMESPACE = new Namespace("notifications");

  // Riak request timeout default is 60s
  private static final int DEFAULT_TIMEOUT_MS = 60000;

  private final RiakClient client;
  private final IdGenerator idGenerator;
  private final CursorStore cursors;
  private final RuleStore ruleStore;

  // timeouts
  private final int timeout;
  private final Duration requestTimeout;

  // timers
  private final Timer fetchTimer;
  private final Timer updateTimer;
  private final Timer deleteTimer;

  private Supplier<ZonedDateTime> currentTimeProvider = () -> ZonedDateTime.now(Clock.systemUTC());

  /**
   * Constructor
   *
   * @param client Riak client
   * @param idGenerator ID Generator
   * @param cursors Cursor data store
   * @param ruleStore Rule data store
   * @param timeout Riak server-side timeout
   * @param requestTimeout Riak client-side timeout
   */
  public NotificationStore(
      final RiakClient client,
      final IdGenerator idGenerator,
      final CursorStore cursors,
      final RuleStore ruleStore,
      final Duration timeout,
      final Duration requestTimeout) {

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate("default");
    this.fetchTimer = registry.timer(MetricRegistry.name(NotificationStore.class, "fetch"));
    this.updateTimer = registry.timer(MetricRegistry.name(NotificationStore.class, "store"));
    this.deleteTimer = registry.timer(MetricRegistry.name(NotificationStore.class, "delete"));

    this.client = Objects.requireNonNull(client, "client == null");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator == null");
    this.cursors = Objects.requireNonNull(cursors, "cursors == null");
    this.ruleStore = Objects.requireNonNull(ruleStore, "ruleStore == null");

    this.timeout =
        Optional.ofNullable(timeout)
            .map(t -> Ints.checkedCast(t.toMilliseconds()))
            .orElse(DEFAULT_TIMEOUT_MS);
    this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout == null");
  }

  /** Internal method to set the allow_multi to true */
  public void initialize() {
    final boolean allowMulti = true;
    LOGGER.debug("Setting allow_multi={} for namespace={}", allowMulti, NAMESPACE);
    final StoreBucketProperties storeBucketProperties =
        new StoreBucketProperties.Builder(NAMESPACE).withAllowMulti(allowMulti).build();

    try {
      client.execute(storeBucketProperties);
    } catch (InterruptedException e) {
      LOGGER.warn(
          String.format("Unable to set allow_multi=%s for namespace=%s", allowMulti, NAMESPACE), e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      LOGGER.error(
          String.format("Unable to set allow_multi=%s for namespace=%s", allowMulti, NAMESPACE), e);
    }
  }

  /**
   * Fetch a list of notifications for a given user
   *
   * @param username User to fetch notifications for
   * @return Optional list of notifications or absent
   * @throws NotificationStoreException if unable to fetch the notifications
   */
  public Optional<UserNotifications> fetch(final String username)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");

    final Location location = new Location(NAMESPACE, username);

    LOGGER.debug("Fetching key (sync): {}", location);

    final NotificationListObject list;
    final FetchValue fv = new FetchValue.Builder(location).withTimeout(timeout).build();
    try (Timer.Context context = fetchTimer.time()) {
      final FetchValue.Response response = client.execute(fv);
      if (response.isNotFound()) {
        return Optional.empty();
      }
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
    }

    if (list == null) {
      return Optional.empty();
    }

    return Optional.of(splitNotifications(username, list.getNotifications()));
  }

  /**
   * Sets the unseen state on all of the notifications based any previously viewed notifications.
   *
   * @param username Username of the notifications
   * @param notifications Original notifications list
   * @return the seen and unseen notifications
   * @throws NotificationStoreException if unable to update the cursor
   */
  public UserNotifications splitNotifications(
      final String username, @Nullable final SortedSet<Notification> notifications)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");

    // if there are no notifications, just return
    if (notifications == null || notifications.isEmpty()) {
      return new UserNotifications();
    }

    // get the ID of the most recent notification (this should never be
    // zero)
    final String newestId = notifications.first().getId("");
    LOGGER.debug("User ({}) newest notification ID: {}", username, newestId);

    // fetch rules from cache
    final Map<String, Rule> rules = ruleStore.fetchCached();
    LOGGER.debug("Fetched {} rules from cache", rules.size());

    final Rollup unseenRollup = new Rollup(rules);

    final Optional<String> cursor = cursors.fetch(username, CURSOR_NAME);
    if (!cursor.isPresent()) {
      // if the user has no cursor, update the cursor to the newest
      // notification
      LOGGER.debug("User ({}) has no cursor, setting to {}", username, newestId);
      cursors.store(username, CURSOR_NAME, newestId);

      // set all of the notifications to unseen=true
      return new UserNotifications(unseenRollup.rollup(setUnseenState(notifications, true)));
    }

    final String lastSeenId = cursor.orElse("");
    LOGGER.debug("User ({}) last seen notification ID: {}", username, lastSeenId);

    // if the latest seen notification ID is less than the newest notification ID, then update the
    // cursor to the newest notification ID.
    if (lastSeenId.compareTo(newestId) < 0) {
      LOGGER.debug("User ({}) updating cursor to {}", username, newestId);
      cursors.store(username, CURSOR_NAME, newestId);
    }

    // get the parent ID of the last seen notification ID
    final Optional<Notification> lastNotification = tryFind(notifications, lastSeenId);
    if (!lastNotification.isPresent()) {
      // if the last notification is not found, set all of the notifications as unseen
      return new UserNotifications(unseenRollup.rollup(setUnseenState(notifications, true)));
    }

    // Set the head of the list as being unseen
    final Stream<Notification> unseen =
        setUnseenState(notifications.headSet(lastNotification.get()), true);

    // Set the tail of the list as being seen
    final Stream<Notification> seen =
        setUnseenState(notifications.tailSet(lastNotification.get()), false);

    final Rollup seenRollup = new Rollup(rules);

    return new UserNotifications(unseenRollup.rollup(unseen), seenRollup.rollup(seen));
  }

  /**
   * Store a new notification for a user
   *
   * @param username User to store the notification
   * @param notification Notification to store
   * @return the stored notification
   * @throws NotificationStoreException if unable to store the notification
   */
  public Notification store(final String username, final Notification notification)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    Objects.requireNonNull(notification, "notification == null");

    final Notification updatedNotification =
        Notification.builder(notification)
            .withId(idGenerator.nextId())
            .withCreatedAt(currentTimeProvider.get())
            .build();

    final NotificationListAddition update = new NotificationListAddition(updatedNotification);

    final Location location = new Location(NAMESPACE, username);
    final UpdateValue updateValue =
        new UpdateValue.Builder(location)
            .withUpdate(update)
            .withStoreOption(StoreValue.Option.RETURN_BODY, false)
            .withTimeout(timeout)
            .build();

    LOGGER.debug("Updating key (sync): {}", location);

    try (Timer.Context context = updateTimer.time()) {
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
   * Asynchronously delete all of the notifications for a given user
   *
   * @param username User to delete all the notifications
   * @throws NotificationStoreException if unable to delete the notifications
   */
  public void removeAll(final String username) throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");

    final Location location = new Location(NAMESPACE, username);
    final DeleteValue deleteValue = new DeleteValue.Builder(location).withTimeout(timeout).build();

    LOGGER.debug("Deleting key (async): {}", location);
    try (Timer.Context context = deleteTimer.time()) {
      final RiakFuture<Void, Location> future = client.executeAsync(deleteValue);
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully deleted key: {}", location);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Delete request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }

    cursors.delete(username, CURSOR_NAME);
  }

  /**
   * Asynchronously remove individual notifications for a given user
   *
   * @param username User to remove notifications from
   * @param ids Notification IDs to remove
   * @throws NotificationStoreException if unable to remove the notifications
   */
  public void remove(final String username, final Collection<String> ids)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    Objects.requireNonNull(ids, "ids == null");

    // if nothing to remove, return early
    if (ids.isEmpty()) {
      return;
    }

    final Location location = new Location(NAMESPACE, username);
    final NotificationListDeletion delete = new NotificationListDeletion(ids);
    final UpdateValue updateValue =
        new UpdateValue.Builder(location)
            .withUpdate(delete)
            .withStoreOption(StoreValue.Option.RETURN_BODY, false)
            .withTimeout(timeout)
            .build();

    LOGGER.debug("Updating key (async): {}", location);
    try (Timer.Context context = updateTimer.time()) {
      final RiakFuture<UpdateValue.Response, Location> future = client.executeAsync(updateValue);
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully updated key: {}", location);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Update request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }
  }

  /**
   * Set the unseen state on multiple notifications
   *
   * @param notifications notifications to update
   * @param unseen whether the notifications have been seen or not
   * @return the updated notifications
   */
  public static Stream<Notification> setUnseenState(
      final Iterable<Notification> notifications, final boolean unseen) {

    return StreamSupport.stream(notifications.spliterator(), false)
        .map(notification -> Notification.builder(notification).withUnseen(unseen).build());
  }

  /**
   * Return the parent notification that matches the given ID or is the parent of a child
   * notification.
   *
   * @param notifications Notifications to search through
   * @param id Notification ID to find
   * @return the notification
   */
  public static Optional<Notification> tryFind(
      final Iterable<Notification> notifications, final String id) {

    Objects.requireNonNull(notifications, "notifications == null");

    if (Strings.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    final com.google.common.base.Optional<Notification> result =
        Iterables.tryFind(
            notifications,
            notification -> {
              // first check that the ID matches
              final Optional<String> notificationId = notification.getId();
              if (!notificationId.isPresent()) {
                return false;
              } else if (id.equals(notificationId.get())) {
                return true;
              }

              // Check to see if the notification is included in any rolled up notifications. This
              // code should not be hit as tryFind() is called prior to the rollups happening, but
              // we include this here for completeness.
              final Collection<Notification> children = notification.getNotifications();
              if (children.isEmpty()) {
                return false;
              }
              return (tryFind(children, id)).isPresent();
            });

    return result.toJavaUtil();
  }

  /**
   * Returns the index in notifications that matches the given ID or is the parent of a child
   * notification, or -1 if the notification was not found.
   *
   * @param notifications Notifications to search through
   * @param id Notification ID to find
   * @return the position of the notification or -1 if not found
   */
  public static int indexOf(final Iterable<Notification> notifications, final String id) {

    Objects.requireNonNull(notifications, "notifications == null");

    if (Strings.isNullOrEmpty(id)) {
      return -1;
    }

    return Iterables.indexOf(
        notifications,
        notification -> {
          // first check that the ID matches
          final Optional<String> notificationId = notification.getId();
          if (!notificationId.isPresent()) {
            return false;
          } else if (id.equals(notificationId.get())) {
            return true;
          }

          // then check to see if the notification is included in any rolled up notifications
          final Collection<Notification> children = notification.getNotifications();
          if (children.isEmpty()) {
            return false;
          }
          return indexOf(children, id) != -1;
        });
  }

  /**
   * Returns an iterable that skips forward to a given notification ID then only returns count more
   * notifications. If the given notification ID is not found
   *
   * @param notifications Iterable of notifications
   * @param startId notification ID to start at
   * @param inclusive Whether to include the startId notification or not
   * @param limitSize Number of notifications to return
   * @return Iterable containing the subset of the original notifications
   */
  public Iterable<Notification> skip(
      final Iterable<Notification> notifications,
      final String startId,
      final boolean inclusive,
      final int limitSize) {

    Objects.requireNonNull(notifications, "notifications == null");

    final int position = indexOf(notifications, startId);
    if (position == -1) {
      return Iterables.limit(notifications, limitSize);
    }
    if (inclusive) {
      return Iterables.limit(Iterables.skip(notifications, position), limitSize);
    }
    return Iterables.limit(Iterables.skip(notifications, position + 1), limitSize);
  }

  /** Set the current time provider for tests */
  @VisibleForTesting
  void setCurrentTimeProvider(Supplier<ZonedDateTime> provider) {
    currentTimeProvider = provider;
  }
}
