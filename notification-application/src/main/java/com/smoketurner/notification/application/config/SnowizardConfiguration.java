package com.smoketurner.notification.application.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SnowizardConfiguration {

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
}
