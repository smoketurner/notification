package com.smoketurner.notification.application.config;

import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.HostAndPort;

public class RiakConfiguration {

    @NotEmpty
    private List<HostAndPort> nodes;

    private String username;

    private String password;

    private String keyStore;

    @Min(1)
    @NotNull
    private Integer minConnections = 1;

    @Min(0)
    @NotNull
    private Integer maxConnections = 0;

    @Min(0)
    @NotNull
    private Integer executionAttempts = 3;

    @NotNull
    @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
    private Duration idleTimeout = Duration.milliseconds(1000);

    @NotNull
    @MinDuration(value = 0, unit = TimeUnit.MILLISECONDS)
    private Duration connectionTimeout = Duration.milliseconds(0);

    @JsonProperty
    public List<HostAndPort> getNodes() {
        return nodes;
    }

    @JsonProperty
    public void setNodes(final List<HostAndPort> nodes) {
        this.nodes = nodes;
    }

    @JsonProperty
    public String getUsername() {
        return username;
    }

    @JsonProperty
    public void setUsername(final String username) {
        this.username = username;
    }

    @JsonProperty
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(final String password) {
        this.password = password;
    }

    @JsonProperty
    public String getKeyStore() {
        return keyStore;
    }

    @JsonProperty
    public void setKeyStore(final String keyStore) {
        this.keyStore = keyStore;
    }

    @JsonProperty
    public Integer getMinConnections() {
        return minConnections;
    }

    @JsonProperty
    public void setMinConnections(final Integer connections) {
        this.minConnections = connections;
    }

    @JsonProperty
    public Integer getMaxConnections() {
        return maxConnections;
    }

    @JsonProperty
    public void setMaxConnections(final Integer connections) {
        this.maxConnections = connections;
    }

    @JsonProperty
    public Integer getExecutionAttempts() {
        return executionAttempts;
    }

    @JsonProperty
    public void setExecutionAttempts(final Integer attempts) {
        this.executionAttempts = attempts;
    }

    @JsonProperty
    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    @JsonProperty
    public void setIdleTimeout(final Duration timeout) {
        this.idleTimeout = timeout;
    }

    @JsonProperty
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    @JsonProperty
    public void setConnectionTimeout(final Duration timeout) {
        this.connectionTimeout = timeout;
    }
}
