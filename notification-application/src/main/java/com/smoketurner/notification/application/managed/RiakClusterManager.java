package com.smoketurner.notification.application.managed;

import static com.google.common.base.Preconditions.checkNotNull;
import io.dropwizard.lifecycle.Managed;
import javax.annotation.Nonnull;
import com.basho.riak.client.core.RiakCluster;

public class RiakClusterManager implements Managed {

    private final RiakCluster cluster;

    /**
     * Constructor
     *
     * @param cluster
     *            Riak cluster instance to manage
     */
    public RiakClusterManager(@Nonnull final RiakCluster cluster) {
        this.cluster = checkNotNull(cluster);
    }

    @Override
    public void start() throws Exception {
        cluster.start();
    }

    @Override
    public void stop() throws Exception {
        cluster.shutdown();
    }
}
