package com.smoketurner.notification.application;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.ConflictResolverFactory;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.basho.riak.client.core.RiakCluster;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ge.snowizard.core.IdWorker;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import com.smoketurner.notification.application.config.RiakClusterFactory;
import com.smoketurner.notification.application.config.SnowizardConfiguration;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
import com.smoketurner.notification.application.filter.CharsetResponseFilter;
import com.smoketurner.notification.application.filter.IdResponseFilter;
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

public class NotificationApplication extends
        Application<NotificationConfiguration> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationApplication.class);

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
        bootstrap
                .setConfigurationSourceProvider(new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor()));
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

        // snowizard
        final SnowizardConfiguration snowizardConfig = configuration
                .getSnowizard();
        final IdWorker snowizard = new IdWorker(snowizardConfig.getWorkerId(),
                snowizardConfig.getDatacenterId(), 0, false, registry);
        LOGGER.info("Worker ID: {}, Datacenter ID: {}",
                snowizardConfig.getWorkerId(),
                snowizardConfig.getDatacenterId());

        // riak
        final RiakClusterFactory factory = new RiakClusterFactory(environment);
        final RiakCluster cluster = factory.build(configuration.getRiak());

        final RiakClient client = new RiakClient(cluster);

        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                NotificationListObject.class, new NotificationListResolver());
        ConflictResolverFactory.INSTANCE.registerConflictResolver(
                CursorObject.class, new CursorResolver());
        ConverterFactory.INSTANCE.registerConverterForClass(
                NotificationListObject.class, new NotificationListConverter());

        environment.healthChecks()
                .register("riak", new RiakHealthCheck(client));

        final CursorStore cursorStore = new CursorStore(registry, client);
        final NotificationStore store = new NotificationStore(registry, client,
                snowizard, cursorStore);
        environment.lifecycle().manage(new CursorStoreManager(cursorStore));
        environment.lifecycle().manage(new NotificationStoreManager(store));

        // resources
        environment.jersey().register(new NotificationResource(store));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
