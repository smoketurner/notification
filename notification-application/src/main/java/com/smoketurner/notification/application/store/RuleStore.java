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
import com.basho.riak.client.api.commands.datatypes.Context;
import com.basho.riak.client.api.commands.datatypes.FetchDatatype;
import com.basho.riak.client.api.commands.datatypes.FetchMap;
import com.basho.riak.client.api.commands.datatypes.MapUpdate;
import com.basho.riak.client.api.commands.datatypes.RegisterUpdate;
import com.basho.riak.client.api.commands.datatypes.UpdateMap;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.crdt.types.RiakMap;
import com.basho.riak.client.core.query.crdt.types.RiakRegister;
import com.basho.riak.client.core.util.BinaryValue;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import io.dropwizard.util.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuleStore.class);
  private static final String BUCKET_NAME = "rules";
  private static final Namespace NAMESPACE = new Namespace("maps", BUCKET_NAME);
  private static final Location LOCATION = new Location(NAMESPACE, BUCKET_NAME);

  // Riak request timeout default is 60s
  private static final int DEFAULT_TIMEOUT_MS = 60000;

  private final RiakClient client;
  private final LoadingCache<String, Map<String, Rule>> cache;

  // timeouts
  private final int timeout;
  private final Duration requestTimeout;

  // metrics
  private final Timer fetchTimer;
  private final Timer storeTimer;
  private final Timer deleteTimer;
  private final Meter cacheMisses;

  /**
   * Constructor
   *
   * @param client Riak client
   * @param cacheTimeout Rule cache refresh timeout
   * @param timeout Riak server-side timeout
   * @param requestTimeout Riak client-side timeout
   */
  public RuleStore(
      final RiakClient client,
      final Duration cacheTimeout,
      final Duration timeout,
      final Duration requestTimeout) {

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate("default");
    this.fetchTimer = registry.timer(MetricRegistry.name(RuleStore.class, "fetch"));
    this.storeTimer = registry.timer(MetricRegistry.name(RuleStore.class, "store"));
    this.deleteTimer = registry.timer(MetricRegistry.name(RuleStore.class, "delete"));
    this.cacheMisses = registry.meter(MetricRegistry.name(RuleStore.class, "cache-misses"));

    this.client = Objects.requireNonNull(client, "client == null");

    this.timeout =
        Optional.ofNullable(timeout)
            .map(t -> Ints.checkedCast(t.toMilliseconds()))
            .orElse(DEFAULT_TIMEOUT_MS);
    this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout == null");

    // set up a cache for the rules
    this.cache =
        CacheBuilder.newBuilder()
            .refreshAfterWrite(cacheTimeout.getQuantity(), cacheTimeout.getUnit())
            .build(
                new CacheLoader<String, Map<String, Rule>>() {
                  @Override
                  public Map<String, Rule> load(String key) throws NotificationStoreException {
                    cacheMisses.mark();

                    // all rules are stored under a common key, so we don't need to reference it
                    return fetch().orElse(Collections.emptyMap());
                  }
                });
  }

  /**
   * Fetch a copy of the rules from the cache.
   *
   * @return the fetched rules or an empty map of rules
   */
  public Map<String, Rule> fetchCached() {
    try {
      return cache.get(BUCKET_NAME);
    } catch (ExecutionException e) {
      LOGGER.warn("Unable to fetch rules from cache, returning no rules", e);
      return Collections.emptyMap();
    }
  }

  /**
   * Fetch the rules from Riak
   *
   * @return the fetched rules
   * @throws NotificationStoreException if unable to fetch the rules
   */
  public Optional<Map<String, Rule>> fetch() throws NotificationStoreException {

    final FetchMap fetchMap =
        new FetchMap.Builder(LOCATION)
            .withOption(FetchDatatype.Option.INCLUDE_CONTEXT, false)
            .withTimeout(timeout)
            .build();

    LOGGER.debug("Fetching key (sync): {}", LOCATION);

    try (Timer.Context context = fetchTimer.time()) {
      final FetchMap.Response response = client.execute(fetchMap);

      final RiakMap map = response.getDatatype();
      return Optional.ofNullable(map).map(m -> getRules(m));
    } catch (ExecutionException e) {
      LOGGER.error("Unable to fetch key: " + LOCATION, e);
      throw new NotificationStoreException(e);
    } catch (InterruptedException e) {
      LOGGER.warn("Fetch request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }
  }

  /**
   * Fetch the context for a rule
   *
   * @return the fetched rule context
   * @throws NotificationStoreException if unable to fetch the rule context
   */
  public Optional<Context> fetchContext() throws NotificationStoreException {
    final FetchMap fetchMap = new FetchMap.Builder(LOCATION).withTimeout(timeout).build();

    LOGGER.debug("Fetching key (sync): {}", LOCATION);

    try (Timer.Context context = fetchTimer.time()) {
      final FetchMap.Response response = client.execute(fetchMap);
      return Optional.ofNullable(response.getContext());
    } catch (ExecutionException e) {
      LOGGER.error("Unable to fetch key: " + LOCATION, e);
      throw new NotificationStoreException(e);
    } catch (InterruptedException e) {
      LOGGER.warn("Fetch request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }
  }

  /**
   * Convert a {@link RiakMap} into a standard map of {@link Rule} objects.
   *
   * @param map the map from Riak to convert
   * @return a map of rule objects where the key is the category
   */
  private static Map<String, Rule> getRules(final RiakMap map) {
    final ImmutableMap.Builder<String, Rule> rules = ImmutableMap.builder();

    for (BinaryValue category : map.view().keySet()) {
      final Rule.Builder builder = Rule.builder();

      final RiakMap properties = map.getMap(category);
      if (properties == null) {
        // should never happen, but avoids a potential NPE below
        continue;
      }

      for (BinaryValue property : properties.view().keySet()) {
        final RiakRegister register = properties.getRegister(property);
        if (register == null) {
          // should never happen, but avoids a potential NPE below
          continue;
        }
        final String value = register.getValue().toString();

        switch (property.toString()) {
          case Rule.MAX_SIZE:
            builder.withMaxSize(Ints.tryParse(value));
            break;
          case Rule.MAX_DURATION:
            try {
              builder.withMaxDuration(Duration.parse(value));
            } catch (IllegalArgumentException e) {
              LOGGER.error("Invalid {} value: {}", Rule.MAX_DURATION, value);
            }
            break;
          case Rule.MATCH_ON:
            builder.withMatchOn(value);
            break;
          default:
            // should never happen
            break;
        }
      }

      final Rule rule = builder.build();
      if (rule.isValid()) {
        rules.put(category.toString(), rule);
      }
    }
    return rules.build();
  }

  /**
   * Asynchronously store a rule
   *
   * @param category Rule category
   * @param rule Rule to store
   * @throws NotificationStoreException if unable to store the rule
   */
  public void store(final String category, final Rule rule) throws NotificationStoreException {

    Objects.requireNonNull(category, "category == null");
    Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");
    Objects.requireNonNull(rule, "rule == null");
    Preconditions.checkState(rule.isValid(), "rule is not valid");

    final Optional<Context> fetchContext = fetchContext();

    final MapUpdate op = new MapUpdate();
    op.update(category, getUpdate(rule, fetchContext));

    final UpdateMap.Builder builder = new UpdateMap.Builder(LOCATION, op).withTimeout(timeout);
    fetchContext.ifPresent(c -> builder.withContext(c));

    LOGGER.debug("Storing key (async): {}", LOCATION);

    try (Timer.Context context = storeTimer.time()) {
      final RiakFuture<UpdateMap.Response, Location> future = client.executeAsync(builder.build());
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully stored key: {}", LOCATION);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Store request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }

    cache.invalidateAll();
  }

  /**
   * Prepare a Riak Map update based on the current state of a Rule
   *
   * @param rule the rule to update
   * @param context Riak context from previous fetch operation
   * @return Riak Map update operation
   */
  private static MapUpdate getUpdate(final Rule rule, final Optional<Context> context) {
    final MapUpdate op = new MapUpdate();
    if (rule.getMaxSize().isPresent()) {
      op.update(Rule.MAX_SIZE, new RegisterUpdate(String.valueOf(rule.getMaxSize().get())));
    } else if (context.isPresent()) {
      op.removeRegister(Rule.MAX_SIZE);
    }
    if (rule.getMaxDuration().isPresent()) {
      op.update(Rule.MAX_DURATION, new RegisterUpdate(rule.getMaxDuration().get().toString()));
    } else if (context.isPresent()) {
      op.removeRegister(Rule.MAX_DURATION);
    }
    if (rule.getMatchOn().isPresent()) {
      op.update(Rule.MATCH_ON, new RegisterUpdate(rule.getMatchOn().get()));
    } else if (context.isPresent()) {
      op.removeRegister(Rule.MATCH_ON);
    }
    return op;
  }

  /**
   * Asynchronously delete a rule
   *
   * @param category Rule category to delete
   * @throws NotificationStoreException if unable to delete the rule
   */
  public void remove(final String category) throws NotificationStoreException {
    Objects.requireNonNull(category, "category == null");
    Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");

    final Optional<Context> fetchContext = fetchContext();
    if (!fetchContext.isPresent()) {
      // if we have no existing context, that means the key didn't exist, so just return.
      return;
    }

    final MapUpdate op = new MapUpdate();
    op.removeMap(category);

    final UpdateMap.Builder builder =
        new UpdateMap.Builder(LOCATION, op).withTimeout(timeout).withContext(fetchContext.get());

    LOGGER.debug("Storing key (async): {}", LOCATION);

    try (Timer.Context context = storeTimer.time()) {
      final RiakFuture<UpdateMap.Response, Location> future = client.executeAsync(builder.build());
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully stored key: {}", LOCATION);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Store request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }

    cache.invalidateAll();
  }

  /**
   * Asynchronously delete all of the rules
   *
   * @throws NotificationStoreException if unable to delete the rules
   */
  public void removeAll() throws NotificationStoreException {
    final DeleteValue deleteValue = new DeleteValue.Builder(LOCATION).withTimeout(timeout).build();

    LOGGER.debug("Deleting key (async): {}", LOCATION);

    try (Timer.Context context = deleteTimer.time()) {
      final RiakFuture<Void, Location> future = client.executeAsync(deleteValue);
      future.await(requestTimeout.getQuantity(), requestTimeout.getUnit());
      if (future.isSuccess()) {
        LOGGER.debug("Successfully deleted key: {}", LOCATION);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Delete request was interrupted", e);
      Thread.currentThread().interrupt();
      throw new NotificationStoreException(e);
    }

    cache.invalidateAll();
  }
}
