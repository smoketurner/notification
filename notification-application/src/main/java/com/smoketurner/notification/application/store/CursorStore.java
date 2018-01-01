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
package com.smoketurner.notification.application.store;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
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
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.riak.CursorObject;
import com.smoketurner.notification.application.riak.CursorUpdate;

public class CursorStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CursorStore.class);
    private static final Namespace NAMESPACE = new Namespace("cursors");
    private final RiakClient client;

    // timers
    private final Timer fetchTimer;
    private final Timer storeTimer;
    private final Timer deleteTimer;

    /**
     * Constructor
     *
     * @param client
     *            Riak client
     */
    public CursorStore(@Nonnull final RiakClient client) {
        final MetricRegistry registry = SharedMetricRegistries
                .getOrCreate("default");
        this.fetchTimer = registry
                .timer(MetricRegistry.name(CursorStore.class, "fetch"));
        this.storeTimer = registry
                .timer(MetricRegistry.name(CursorStore.class, "store"));
        this.deleteTimer = registry
                .timer(MetricRegistry.name(CursorStore.class, "delete"));

        this.client = Objects.requireNonNull(client, "client == null");
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
                    "Unable to set allow_multi=%s for namespace=%s", allowMulti,
                    NAMESPACE), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error(String.format(
                    "Unable to set allow_multi=%s for namespace=%s", allowMulti,
                    NAMESPACE), e);
        }
    }

    /**
     * Fetch the cursor for a given user
     *
     * @param username
     *            User to get the cursor for
     * @param cursorName
     *            Name of the cursor to fetch
     * @return the last seen notification ID
     * @throws NotificationStoreException
     *             if unable to fetch the cursor
     */
    public Optional<Long> fetch(@Nonnull final String username,
            @Nonnull final String cursorName)
            throws NotificationStoreException {

        Objects.requireNonNull(username, "username == null");
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Objects.requireNonNull(cursorName, "cursorName == null");
        Preconditions.checkArgument(!cursorName.isEmpty(),
                "cursorName cannot be empty");

        final String key = getCursorKey(username, cursorName);
        final Location location = new Location(NAMESPACE, key);

        LOGGER.debug("Fetching key: {}", location);

        final CursorObject cursor;
        final FetchValue fv = new FetchValue.Builder(location).build();
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

        if (cursor == null) {
            return Optional.empty();
        }
        return Optional.of(cursor.getValue());
    }

    /**
     * Asynchronously update a given cursor with the specified value.
     *
     * @param username
     *            Username to update the cursor for
     * @param cursorName
     *            Name of the cursor to store
     * @param value
     *            Value to set
     */
    public void store(@Nonnull final String username,
            @Nonnull final String cursorName, final long value) {

        Objects.requireNonNull(username, "username == null");
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Objects.requireNonNull(cursorName, "cursorName == null");
        Preconditions.checkArgument(!cursorName.isEmpty(),
                "cursorName cannot be empty");

        final String key = getCursorKey(username, cursorName);
        final CursorUpdate update = new CursorUpdate(key, value);

        final Location location = new Location(NAMESPACE, key);
        final UpdateValue updateValue = new UpdateValue.Builder(location)
                .withUpdate(update)
                .withStoreOption(StoreValue.Option.RETURN_BODY, false).build();

        LOGGER.debug("Updating key ({}) to value (async): {}", location, value);
        try (Timer.Context context = storeTimer.time()) {
            client.executeAsync(updateValue);
        }
    }

    /**
     * Asynchronously delete the cursor for a given user
     * 
     * @param username
     *            User delete their cursor
     * @param cursorName
     *            Name of the cursor
     */
    public void delete(@Nonnull final String username,
            @Nonnull final String cursorName) {

        Objects.requireNonNull(username, "username == null");
        Preconditions.checkArgument(!username.isEmpty(),
                "username cannot be empty");
        Objects.requireNonNull(cursorName, "cursorName == null");
        Preconditions.checkArgument(!cursorName.isEmpty(),
                "cursorName cannot be empty");

        final String key = getCursorKey(username, cursorName);
        final Location location = new Location(NAMESPACE, key);
        final DeleteValue deleteValue = new DeleteValue.Builder(location)
                .build();

        LOGGER.debug("Deleting key (async): {}", location);
        try (Timer.Context context = deleteTimer.time()) {
            client.executeAsync(deleteValue);
        }
    }

    /**
     * Return the key name for fetching a cursor
     *
     * @param username
     *            Username to fetch
     * @param cursorName
     *            Name of the cursor to fetch
     * @return the key name
     */
    public String getCursorKey(@Nonnull final String username,
            @Nonnull final String cursorName) {
        return username + "-" + cursorName;
    }
}
