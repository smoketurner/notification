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
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.CursorObject;
import com.smoketurner.notification.application.riak.CursorUpdate;
import io.dropwizard.util.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CursorStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(CursorStore.class);
  private static final Namespace NAMESPACE = new Namespace("cursors");

  // Riak request timeout default is 60s
  private static final int DEFAULT_TIMEOUT_MS = 60000;
  private final RiakClient client;

  // timeouts
  private final int timeout;
  private final Duration requestTimeout;

  // timers
  private final Timer fetchTimer;
  private final Timer storeTimer;
  private final Timer deleteTimer;

  /**
   * Constructor
   *
   * @param client Riak client
   * @param timeout Riak server-side timeout
   * @param requestTimeout Riak client-side timeout
   */
  public CursorStore(
      final RiakClient client, final Duration timeout, final Duration requestTimeout) {

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate("default");
    this.fetchTimer = registry.timer(MetricRegistry.name(CursorStore.class, "fetch"));
    this.storeTimer = registry.timer(MetricRegistry.name(CursorStore.class, "store"));
    this.deleteTimer = registry.timer(MetricRegistry.name(CursorStore.class, "delete"));

    this.client = Objects.requireNonNull(client, "client == null");

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
   * Fetch the cursor for a given user
   *
   * @param username User to get the cursor for
   * @param cursorName Name of the cursor to fetch
   * @return the last seen notification ID
   * @throws NotificationStoreException if unable to fetch the cursor
   */
  public Optional<String> fetch(final String username, final String cursorName)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    Objects.requireNonNull(cursorName, "cursorName == null");
    Preconditions.checkArgument(!cursorName.isEmpty(), "cursorName cannot be empty");

    final String key = getCursorKey(username, cursorName);
    final Location location = new Location(NAMESPACE, key);

    LOGGER.debug("Fetching key (sync): {}", location);

    final CursorObject cursor;
    final FetchValue fv = new FetchValue.Builder(location).withTimeout(timeout).build();
    try (Timer.Context context = fetchTimer.time()) {
      final FetchValue.Response response = client.execute(fv);
      if (response.isNotFound()) {
        return Optional.empty();
      }
      cursor = response.getValue(CursorObject.class);
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

    return Optional.ofNullable(cursor).map(c -> c.getValue());
  }

  /**
   * Asynchronously update a given cursor with the specified value.
   *
   * @param username Username to update the cursor for
   * @param cursorName Name of the cursor to store
   * @param value Value to set
   * @throws NotificationStoreException if unable to update the cursor
   */
  public void store(final String username, final String cursorName, final String value)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    Objects.requireNonNull(cursorName, "cursorName == null");
    Preconditions.checkArgument(!cursorName.isEmpty(), "cursorName cannot be empty");

    final String key = getCursorKey(username, cursorName);
    final CursorUpdate update = new CursorUpdate(key, value);

    final Location location = new Location(NAMESPACE, key);
    final UpdateValue updateValue =
        new UpdateValue.Builder(location)
            .withUpdate(update)
            .withStoreOption(StoreValue.Option.RETURN_BODY, false)
            .withTimeout(timeout)
            .build();

    LOGGER.debug("Updating cursor ({}) to value (async): {}", location, value);
    try (Timer.Context context = storeTimer.time()) {
      final RiakFuture<UpdateValue.Response, Location> future = client.executeAsync(updateValue);
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully updated cursor: {}", location);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Update request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }
  }

  /**
   * Asynchronously delete the cursor for a given user
   *
   * @param username User delete their cursor
   * @param cursorName Name of the cursor
   * @throws NotificationStoreException if unable to delete the cursor
   */
  public void delete(final String username, final String cursorName)
      throws NotificationStoreException {

    Objects.requireNonNull(username, "username == null");
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    Objects.requireNonNull(cursorName, "cursorName == null");
    Preconditions.checkArgument(!cursorName.isEmpty(), "cursorName cannot be empty");

    final String key = getCursorKey(username, cursorName);
    final Location location = new Location(NAMESPACE, key);
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
  }

  /**
   * Return the key name for fetching a cursor
   *
   * @param username Username to fetch
   * @param cursorName Name of the cursor to fetch
   * @return the key name
   */
  public String getCursorKey(final String username, final String cursorName) {
    return username + "-" + cursorName;
  }
}
