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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import com.smoketurner.notification.application.resources.NotificationResource;
import com.smoketurner.notification.application.resources.PingResource;
import com.smoketurner.notification.application.resources.VersionResource;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;

public class NotificationApplicationTest {
    private final MetricRegistry registry = new MetricRegistry();
    private final ObjectMapper mapper = Jackson.newObjectMapper();
    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    private final LifecycleEnvironment lifecycle = mock(
            LifecycleEnvironment.class);
    private final HealthCheckRegistry healthChecks = mock(
            HealthCheckRegistry.class);
    private final NotificationApplication application = new NotificationApplication();
    private final NotificationConfiguration config = new NotificationConfiguration();

    @Before
    public void setup() throws Exception {
        config.getSnowizard().setDatacenterId(1);
        config.getSnowizard().setWorkerId(1);
        when(environment.metrics()).thenReturn(registry);
        when(environment.jersey()).thenReturn(jersey);
        when(environment.getObjectMapper()).thenReturn(mapper);
        when(environment.lifecycle()).thenReturn(lifecycle);
        when(environment.healthChecks()).thenReturn(healthChecks);
    }

    @Test
    public void buildsAVersionResource() throws Exception {
        application.run(config, environment);
        verify(jersey).register(isA(VersionResource.class));
    }

    @Test
    public void buildsAPingResource() throws Exception {
        application.run(config, environment);
        verify(jersey).register(isA(PingResource.class));
    }

    @Test
    public void buildsANotificationResource() throws Exception {
        application.run(config, environment);
        verify(jersey).register(isA(NotificationResource.class));
    }
}
