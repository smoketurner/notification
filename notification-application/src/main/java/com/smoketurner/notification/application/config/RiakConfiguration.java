/**
 * Copyright 2016 Smoke Turner, LLC.
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

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.util.DefaultCharset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.Ints;
import com.smoketurner.notification.application.health.RiakHealthCheck;
import com.smoketurner.notification.application.managed.RiakClusterManager;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;

public class RiakConfiguration {

    @NotEmpty
    private List<HostAndPort> nodes = Collections.emptyList();

    private String username;

    private String password;

    private String keyStore;

    @Min(1)
    @NotNull
    private Integer minConnections = RiakNode.Builder.DEFAULT_MIN_CONNECTIONS;

    @Min(0)
    @NotNull
    private Integer maxConnections = RiakNode.Builder.DEFAULT_MAX_CONNECTIONS;

    @Min(0)
    @NotNull
    private Integer executionAttempts = RiakCluster.Builder.DEFAULT_EXECUTION_ATTEMPTS;

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

    @JsonIgnore
    public RiakClient build(@Nonnull final Environment environment)
            throws UnknownHostException, KeyStoreException {
        Objects.requireNonNull(environment);

        final RiakNode.Builder builder = new RiakNode.Builder()
                .withMinConnections(minConnections)
                .withMaxConnections(maxConnections)
                .withConnectionTimeout(
                        Ints.checkedCast(connectionTimeout.toMilliseconds()))
                .withIdleTimeout(Ints.checkedCast(idleTimeout.toMilliseconds()))
                .withBlockOnMaxConnections(false);

        if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)
                && !Strings.isNullOrEmpty(keyStore)) {
            // TODO finish keyStore implementation
            final KeyStore keystore = KeyStore.getInstance("PKS");
            builder.withAuth(username, password, keystore);
        }

        final List<RiakNode> nodes = new ArrayList<>();
        for (HostAndPort address : this.nodes) {
            final RiakNode node = builder
                    .withRemoteAddress(address.getHostText())
                    .withRemotePort(address.getPortOrDefault(
                            RiakNode.Builder.DEFAULT_REMOTE_PORT))
                    .build();
            nodes.add(node);
        }

        DefaultCharset.set(StandardCharsets.UTF_8);

        final RiakCluster cluster = RiakCluster.builder(nodes)
                .withExecutionAttempts(executionAttempts).build();
        environment.lifecycle().manage(new RiakClusterManager(cluster));

        final RiakClient client = new RiakClient(cluster);
        environment.healthChecks().register("riak",
                new RiakHealthCheck(client));
        return client;
    }
}
