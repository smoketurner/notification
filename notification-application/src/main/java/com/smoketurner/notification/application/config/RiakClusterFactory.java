package com.smoketurner.notification.application.config;

import io.dropwizard.setup.Environment;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import jersey.repackaged.com.google.common.collect.Lists;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.smoketurner.notification.application.managed.RiakClusterManager;

public class RiakClusterFactory {

    private static final int DEFAULT_RIAK_PORT = 8087;
    private final Environment environment;

    /**
     * Constructor
     *
     * @param environment
     */
    public RiakClusterFactory(@Nonnull final Environment environment) {
        this.environment = Preconditions.checkNotNull(environment);
    }

    public RiakCluster build(final RiakConfiguration config)
            throws UnknownHostException, KeyStoreException {
        final RiakNode.Builder builder = new RiakNode.Builder()
                .withMinConnections(config.getMinConnections())
                .withConnectionTimeout(
                        (int) config.getConnectionTimeout().toMilliseconds())
                .withIdleTimeout((int) config.getIdleTimeout().toMilliseconds())
                .withBlockOnMaxConnections(false);
        if (config.getMaxConnections() > 0) {
            builder.withMaxConnections(config.getMaxConnections());
        }
        if (!Strings.isNullOrEmpty(config.getUsername())
                && !Strings.isNullOrEmpty(config.getPassword())
                && !Strings.isNullOrEmpty(config.getKeyStore())) {
            final KeyStore keystore = KeyStore.getInstance("PKS");
            builder.withAuth(config.getUsername(), config.getPassword(),
                    keystore);
        }

        final LinkedList<RiakNode> nodes = Lists.newLinkedList();
        for (HostAndPort address : config.getNodes()) {
            final RiakNode node = builder
                    .withRemoteAddress(address.getHostText())
                    .withRemotePort(address.getPortOrDefault(DEFAULT_RIAK_PORT))
                    .build();
            nodes.add(node);
        }

        final RiakCluster cluster = RiakCluster.builder(nodes)
                .withExecutionAttempts(config.getExecutionAttempts()).build();
        environment.lifecycle().manage(new RiakClusterManager(cluster));
        return cluster;
    }
}
