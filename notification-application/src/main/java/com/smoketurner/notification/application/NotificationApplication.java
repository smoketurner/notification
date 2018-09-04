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
package com.smoketurner.notification.application;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.ConflictResolverFactory;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smoketurner.dropwizard.graphql.GraphQLBundle;
import com.smoketurner.dropwizard.graphql.GraphQLFactory;
import com.smoketurner.dropwizard.riak.RiakBundle;
import com.smoketurner.dropwizard.riak.RiakFactory;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import com.smoketurner.notification.application.core.IdGenerator;
import com.smoketurner.notification.application.core.WebSecurityFilter;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
import com.smoketurner.notification.application.graphql.CreateNotificationMutation;
import com.smoketurner.notification.application.graphql.CreateRuleMutation;
import com.smoketurner.notification.application.graphql.NotificationDataFetcher;
import com.smoketurner.notification.application.graphql.RemoveAllNotificationsMutation;
import com.smoketurner.notification.application.graphql.RemoveAllRulesMutation;
import com.smoketurner.notification.application.graphql.RemoveNotificationMutation;
import com.smoketurner.notification.application.graphql.RemoveRuleMutation;
import com.smoketurner.notification.application.graphql.RuleDataFetcher;
import com.smoketurner.notification.application.graphql.Scalars;
import com.smoketurner.notification.application.graphql.UsernameFieldValidation;
import com.smoketurner.notification.application.managed.CursorStoreManager;
import com.smoketurner.notification.application.managed.NotificationStoreManager;
import com.smoketurner.notification.application.resources.NotificationResource;
import com.smoketurner.notification.application.resources.PingResource;
import com.smoketurner.notification.application.resources.RuleResource;
import com.smoketurner.notification.application.resources.VersionResource;
import com.smoketurner.notification.application.riak.CursorObject;
import com.smoketurner.notification.application.riak.CursorResolver;
import com.smoketurner.notification.application.riak.NotificationListConverter;
import com.smoketurner.notification.application.riak.NotificationListObject;
import com.smoketurner.notification.application.riak.NotificationListResolver;
import com.smoketurner.notification.application.store.CursorStore;
import com.smoketurner.notification.application.store.NotificationStore;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation;
import graphql.schema.idl.RuntimeWiring;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.filter.CharsetUtf8Filter;
import io.dropwizard.jersey.filter.RequestIdFilter;
import io.dropwizard.jersey.filter.RuntimeFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationApplication extends Application<NotificationConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationApplication.class);
  private static final AtomicReference<NotificationStore> NOTIFICATION_STORE =
      new AtomicReference<>();
  private static final AtomicReference<RuleStore> RULE_STORE = new AtomicReference<>();
  private static final AtomicReference<CursorStore> CURSOR_STORE = new AtomicReference<>();

  public static void main(final String[] args) throws Exception {
    // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-jvm-ttl.html
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    new NotificationApplication().run(args);
  }

  @Override
  public String getName() {
    return "notification";
  }

  @Override
  public void initialize(Bootstrap<NotificationConfiguration> bootstrap) {
    ConflictResolverFactory.INSTANCE.registerConflictResolver(
        NotificationListObject.class, new NotificationListResolver());
    ConflictResolverFactory.INSTANCE.registerConflictResolver(
        CursorObject.class, new CursorResolver());
    ConverterFactory.INSTANCE.registerConverterForClass(
        NotificationListObject.class, new NotificationListConverter());

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    // add Riak bundle
    bootstrap.addBundle(
        new RiakBundle<NotificationConfiguration>() {
          @Override
          public RiakFactory getRiakFactory(final NotificationConfiguration configuration) {
            return configuration.getRiak();
          }
        });

    bootstrap.addBundle(
        new GraphQLBundle<NotificationConfiguration>() {
          @Override
          public GraphQLFactory getGraphQLFactory(final NotificationConfiguration configuration) {
            final GraphQLFactory factory = configuration.getGraphQL();
            try {
              factory.setRuntimeWiring(buildWiring(configuration));
            } catch (Exception e) {
              LOGGER.error("Unable to build RuntimeWiring", e);
            }

            final FieldValidationInstrumentation instrumentation =
                new FieldValidationInstrumentation(new UsernameFieldValidation());
            factory.setInstrumentations(Collections.singletonList(instrumentation));

            return factory;
          }
        });
  }

  @Override
  public void run(final NotificationConfiguration configuration, final Environment environment)
      throws Exception {

    // returns all DateTime objects as ISO8601 strings
    environment.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    environment.jersey().register(NotificationExceptionMapper.class);
    // adds charset=UTF-8 to the response headers
    environment.jersey().register(CharsetUtf8Filter.class);
    // adds a X-Request-Id response header
    environment.jersey().register(RequestIdFilter.class);
    // adds a X-Runtime response header
    environment.jersey().register(RuntimeFilter.class);
    environment.jersey().register(WebSecurityFilter.class);

    // riak
    final RiakClient client = configuration.getRiak().build();

    // data stores
    final RuleStore ruleStore = getRuleStore(client, configuration);
    final CursorStore cursorStore = getCursorStore(client, configuration);
    final NotificationStore store = getNotificationStore(client, configuration);

    environment.lifecycle().manage(new CursorStoreManager(cursorStore));
    environment.lifecycle().manage(new NotificationStoreManager(store));

    // resources
    environment.jersey().register(new NotificationResource(store));
    environment.jersey().register(new RuleResource(ruleStore));
    environment.jersey().register(new PingResource());
    environment.jersey().register(new VersionResource());
  }

  /**
   * Get and configure the {@link RuleStore}
   *
   * @param client Riak client
   * @param configuration Notification configuration
   * @return RuleStore
   */
  private static RuleStore getRuleStore(
      final RiakClient client, final NotificationConfiguration configuration) {

    if (RULE_STORE.get() != null) {
      return RULE_STORE.get();
    }

    final RuleStore store =
        new RuleStore(
            client,
            configuration.getRuleCacheTimeout(),
            configuration.getRiakTimeout(),
            configuration.getRiakRequestTimeout());
    if (RULE_STORE.compareAndSet(null, store)) {
      return store;
    }
    return getRuleStore(client, configuration);
  }

  /**
   * Get and configure the {@link CursorStore}
   *
   * @param client Riak client
   * @param configuration Notification configuration
   * @return CursorStore
   */
  private static CursorStore getCursorStore(
      final RiakClient client, final NotificationConfiguration configuration) {

    if (CURSOR_STORE.get() != null) {
      return CURSOR_STORE.get();
    }

    final CursorStore store =
        new CursorStore(
            client, configuration.getRiakTimeout(), configuration.getRiakRequestTimeout());
    if (CURSOR_STORE.compareAndSet(null, store)) {
      return store;
    }
    return getCursorStore(client, configuration);
  }

  /**
   * Get and configure the {@link NotificationStore}
   *
   * @param client Riak client
   * @param configuration Notification configuration
   * @return NotificationStore
   */
  private static NotificationStore getNotificationStore(
      final RiakClient client, final NotificationConfiguration configuration) {

    if (NOTIFICATION_STORE.get() != null) {
      return NOTIFICATION_STORE.get();
    }

    // KSUID
    final IdGenerator idGenerator = new IdGenerator();

    final CursorStore cursorStore = getCursorStore(client, configuration);
    final RuleStore ruleStore = getRuleStore(client, configuration);

    final NotificationStore store =
        new NotificationStore(
            client,
            idGenerator,
            cursorStore,
            ruleStore,
            configuration.getRiakTimeout(),
            configuration.getRiakRequestTimeout());
    if (NOTIFICATION_STORE.compareAndSet(null, store)) {
      return store;
    }
    return getNotificationStore(client, configuration);
  }

  /**
   * Build the GraphQL {@link RuntimeWiring}
   *
   * @param configuration Notification configuration
   * @return the GraphQL runtime wiring
   * @throws Exception if unable to connect to Riak
   */
  private static RuntimeWiring buildWiring(NotificationConfiguration configuration)
      throws Exception {

    final RiakClient client = configuration.getRiak().build();
    final NotificationStore store = getNotificationStore(client, configuration);
    final RuleStore ruleStore = getRuleStore(client, configuration);

    final RuntimeWiring wiring =
        RuntimeWiring.newRuntimeWiring()
            .scalar(Scalars.graphQLMapScalar("Map"))
            .type(
                "Query",
                typeWiring ->
                    typeWiring
                        .dataFetcher("notifications", new NotificationDataFetcher(store))
                        .dataFetcher("rules", new RuleDataFetcher(ruleStore)))
            .type(
                "Mutation",
                typeWiring ->
                    typeWiring
                        .dataFetcher("createNotification", new CreateNotificationMutation(store))
                        .dataFetcher("removeNotification", new RemoveNotificationMutation(store))
                        .dataFetcher(
                            "removeAllNotifications", new RemoveAllNotificationsMutation(store))
                        .dataFetcher("createRule", new CreateRuleMutation(ruleStore))
                        .dataFetcher("removeRule", new RemoveRuleMutation(ruleStore))
                        .dataFetcher("removeAllRules", new RemoveAllRulesMutation(ruleStore)))
            .build();

    return wiring;
  }
}
