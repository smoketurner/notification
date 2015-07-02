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
