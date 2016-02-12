/**
 * Copyright 2016 Smoke Turner, LLC.
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
package com.smoketurner.notification.application;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.ConflictResolverFactory;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.basho.riak.client.core.RiakCluster;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ge.snowizard.core.IdWorker;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import com.smoketurner.notification.application.config.RiakConfiguration;
import com.smoketurner.notification.application.core.IdGenerator;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
import com.smoketurner.notification.application.filter.CharsetResponseFilter;
import com.smoketurner.notification.application.filter.IdResponseFilter;
import com.smoketurner.notification.application.filter.RuntimeFilter;
import com.smoketurner.notification.application.health.RiakHealthCheck;
import com.smoketurner.notification.application.managed.CursorStoreManager;
import com.smoketurner.notification.application.managed.NotificationStoreManager;
import com.smoketurner.notification.application.resources.NotificationResource;
import com.smoketurner.notification.application.resources.PingResource;
import com.smoketurner.notification.application.resources.VersionResource;
import com.smoketurner.notification.application.riak.CursorObject;
import com.smoketurner.notification.application.riak.CursorResolver;
import com.smoketurner.notification.application.riak.NotificationListConverter;
import com.smoketurner.notification.application.riak.NotificationListObject;
import com.smoketurner.notification.application.riak.NotificationListResolver;
import com.smoketurner.notification.application.store.CursorStore;
import com.smoketurner.notification.application.store.NotificationStore;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class NotificationApplication
        extends Application<NotificationConfiguration> {

    public static void main(final String[] args) throws Exception {
        new NotificationApplication().run(args);
    }

    @Override
    public String getName() {
        return "notification";
    }

    @Override
    public void initialize(Bootstrap<NotificationConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));

        bootstrap.addBundle(new SwaggerBundle<NotificationConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final NotificationConfiguration configuration) {
                return configuration.getSwagger();
            }
        });
    }

    @Override
    public void run(final NotificationConfiguration configuration,
            final Environment environment) throws Exception {

        final MetricRegistry registry = environment.metrics();

        // returns all DateTime objects as ISO8601 strings
        environment.getObjectMapper().configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        environment.jersey().register(NotificationExceptionMapper.class);
        // adds charset=UTF-8 to the response headers
        environment.jersey().register(CharsetResponseFilter.class);
        // adds a Request-Id response header
        environment.jersey().register(IdResponseFilter.class);
        // adds a X-Runtime response header
        environment.jersey().register(RuntimeFilter.class);

        // snowizard
        final IdWorker snowizard = configuration.getSnowizard()
                .build(environment);
        final IdGenerator idGenerator = new IdGenerator(snowizard,
                configuration.getSnowizard().isEnabled());

        // riak
        final RiakConfiguration riakConfig = configuration.getRiak();
        final RiakCluster cluster = riakConfig.build(environment);
        final RiakClient client = new RiakClient(cluster);

        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                NotificationListObject.class, new NotificationListResolver());
        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                CursorObject.class, new CursorResolver());
        ConverterFactory.INSTANCE.registerConverterForClass(
                NotificationListObject.class, new NotificationListConverter());

        environment.healthChecks().register("riak",
                new RiakHealthCheck(client));

        // data stores
        final CursorStore cursorStore = new CursorStore(registry, client);
        final NotificationStore store = new NotificationStore(registry, client,
                idGenerator, cursorStore, configuration.getRules());
        environment.lifecycle().manage(new CursorStoreManager(cursorStore));
        environment.lifecycle().manage(new NotificationStoreManager(store));

        // resources
        environment.jersey().register(new NotificationResource(store));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
