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
package com.smoketurner.notification.application.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.notification.application.NotificationApplication;
import com.smoketurner.snowizard.core.IdWorker;
import io.dropwizard.setup.Environment;

public class SnowizardConfiguration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SnowizardConfiguration.class);

    @Min(1)
    @Max(31)
    private int datacenterId;

    @Min(1)
    @Max(31)
    private int workerId;

    private boolean enabled = false;

    @JsonProperty
    public int getDatacenterId() {
        return datacenterId;
    }

    @JsonProperty
    public void setDatacenterId(final int datacenterId) {
        this.datacenterId = datacenterId;
    }

    @JsonProperty
    public int getWorkerId() {
        return workerId;
    }

    @JsonProperty
    public void setWorkerId(final int workerId) {
        this.workerId = workerId;
    }

    @JsonProperty
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @JsonIgnore
    public IdWorker build(@Nonnull final Environment environment) {
        Objects.requireNonNull(environment);
        final MetricRegistry registry = environment.metrics();

        registry.register(
                MetricRegistry.name(NotificationApplication.class, "worker_id"),
                (Gauge<Integer>) this::getWorkerId);

        registry.register(
                MetricRegistry.name(NotificationApplication.class,
                        "datacenter_id"),
                (Gauge<Integer>) this::getDatacenterId);

        LOGGER.info("Worker ID: {}, Datacenter ID: {}", workerId, datacenterId);

        return IdWorker.builder(workerId, datacenterId)
                .withMetricRegistry(registry).withValidateUserAgent(false)
                .build();
    }
}
