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

import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.riak.RiakFactory;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class NotificationConfiguration extends Configuration {

    @NotNull
    @MinDuration(value = 1, unit = TimeUnit.SECONDS)
    private Duration ruleCacheTimeout = Duration.seconds(30);

    @Valid
    @NotNull
    @JsonProperty
    public final SwaggerBundleConfiguration swagger = new SwaggerBundleConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final RiakFactory riak = new RiakFactory();

    @Valid
    @NotNull
    @JsonProperty
    private final SnowizardConfiguration snowizard = new SnowizardConfiguration();

    @JsonProperty
    public Duration getRuleCacheTimeout() {
        return ruleCacheTimeout;
    }

    @JsonProperty
    public void setRuleCacheTimeout(final Duration timeout) {
        this.ruleCacheTimeout = timeout;
    }

    @JsonProperty
    public SwaggerBundleConfiguration getSwagger() {
        return swagger;
    }

    @JsonProperty
    public RiakFactory getRiak() {
        return riak;
    }

    @JsonProperty
    public SnowizardConfiguration getSnowizard() {
        return snowizard;
    }
}
