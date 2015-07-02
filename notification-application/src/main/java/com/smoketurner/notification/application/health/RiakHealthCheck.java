package com.smoketurner.notification.application.health;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.List;
import javax.annotation.Nonnull;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakNode;
import com.codahale.metrics.health.HealthCheck;

public class RiakHealthCheck extends HealthCheck {

    private final RiakClient client;

    /**
     * Constructor
     *
     * @param client
     *            Riak client
     */
    public RiakHealthCheck(@Nonnull final RiakClient client) {
        this.client = checkNotNull(client);
    }

    @Override
    protected Result check() throws Exception {
        final List<RiakNode> nodes = client.getRiakCluster().getNodes();
        if (nodes.size() > 0) {
            return Result.healthy();
        }
        return Result.unhealthy("No available Riak nodes");
    }
}
