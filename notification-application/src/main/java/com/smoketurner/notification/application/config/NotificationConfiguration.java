/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.config;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty
    private final RiakConfiguration riak = new RiakConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final SnowizardConfiguration snowizard = new SnowizardConfiguration();

    @JsonProperty
    public RiakConfiguration getRiak() {
        return riak;
    }

    @JsonProperty
    public SnowizardConfiguration getSnowizard() {
        return snowizard;
    }
}
