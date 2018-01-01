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
package com.smoketurner.notification.application;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.ConflictResolverFactory;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smoketurner.dropwizard.riak.RiakBundle;
import com.smoketurner.dropwizard.riak.RiakFactory;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import com.smoketurner.notification.application.core.IdGenerator;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
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
import com.smoketurner.snowizard.core.IdWorker;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.filter.CharsetUtf8Filter;
import io.dropwizard.jersey.filter.RequestIdFilter;
import io.dropwizard.jersey.filter.RuntimeFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class NotificationApplication
        extends Application<NotificationConfiguration> {

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
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));

        // add Swagger bundle
        bootstrap.addBundle(new SwaggerBundle<NotificationConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final NotificationConfiguration configuration) {
                return configuration.getSwagger();
            }
        });

        // add Riak bundle
        bootstrap.addBundle(new RiakBundle<NotificationConfiguration>() {
            @Override
            public RiakFactory getRiakFactory(
                    final NotificationConfiguration configuration) {
                return configuration.getRiak();
            }
        });
    }

    @Override
    public void run(final NotificationConfiguration configuration,
            final Environment environment) throws Exception {

        // returns all DateTime objects as ISO8601 strings
        environment.getObjectMapper().configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        environment.jersey().register(NotificationExceptionMapper.class);
        // adds charset=UTF-8 to the response headers
        environment.jersey().register(CharsetUtf8Filter.class);
        // adds a X-Request-Id response header
        environment.jersey().register(RequestIdFilter.class);
        // adds a X-Runtime response header
        environment.jersey().register(RuntimeFilter.class);

        // snowizard
        final IdWorker snowizard = configuration.getSnowizard()
                .build(environment);
        final IdGenerator idGenerator = new IdGenerator(snowizard,
                configuration.getSnowizard().isEnabled());

        // riak
        final RiakClient client = configuration.getRiak().build();

        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                NotificationListObject.class, new NotificationListResolver());
        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                CursorObject.class, new CursorResolver());
        ConverterFactory.INSTANCE.registerConverterForClass(
                NotificationListObject.class, new NotificationListConverter());

        // data stores
        final RuleStore ruleStore = new RuleStore(client,
                configuration.getRuleCacheTimeout());
        final CursorStore cursorStore = new CursorStore(client);
        final NotificationStore store = new NotificationStore(client,
                idGenerator, cursorStore, ruleStore);
        environment.lifecycle().manage(new CursorStoreManager(cursorStore));
        environment.lifecycle().manage(new NotificationStoreManager(store));

        // resources
        environment.jersey().register(new NotificationResource(store));
        environment.jersey().register(new RuleResource(ruleStore));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
