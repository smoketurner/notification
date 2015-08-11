/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.notification.application.config;

import io.dropwizard.setup.Environment;

import javax.annotation.Nonnull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ge.snowizard.core.IdWorker;
import com.google.common.base.Preconditions;
import com.smoketurner.notification.application.NotificationApplication;

public class SnowizardConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(SnowizardConfiguration.class);

  @Min(1)
  @Max(31)
  @NotNull
  private Integer datacenterId;

  @Min(1)
  @Max(31)
  @NotNull
  private Integer workerId;

  @JsonProperty
  public Integer getDatacenterId() {
    return datacenterId;
  }

  @JsonProperty
  public void setDatacenterId(final int datacenterId) {
    this.datacenterId = datacenterId;
  }

  @JsonProperty
  public Integer getWorkerId() {
    return workerId;
  }

  @JsonProperty
  public void setWorkerId(final int workerId) {
    this.workerId = workerId;
  }

  @JsonIgnore
  public IdWorker build(@Nonnull final Environment environment) {
    Preconditions.checkNotNull(environment);
    final MetricRegistry registry = environment.metrics();

    registry.register(MetricRegistry.name(NotificationApplication.class, "worker_id"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return workerId;
          }
        });

    registry.register(MetricRegistry.name(NotificationApplication.class, "datacenter_id"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return datacenterId;
          }
        });

    LOGGER.info("Worker ID: {}, Datacenter ID: {}", workerId, datacenterId);

    return new IdWorker(workerId, datacenterId, 0, false, registry);
  }
}
