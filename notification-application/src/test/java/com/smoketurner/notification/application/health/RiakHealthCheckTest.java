package com.smoketurner.notification.application.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.codahale.metrics.health.HealthCheck.Result;
import com.google.common.collect.ImmutableList;

public class RiakHealthCheckTest {

    private final RiakClient client = mock(RiakClient.class);
    private final RiakCluster cluster = mock(RiakCluster.class);
    private RiakHealthCheck check;

    @Before
    public void setUp() throws Exception {
        when(client.getRiakCluster()).thenReturn(cluster);
        check = new RiakHealthCheck(client);
    }

    @After
    public void tearDown() {
        reset(cluster);
    }

    @Test
    public void testCheckHealthy() throws Exception {
        final List<RiakNode> nodes = ImmutableList.of(new RiakNode.Builder()
                .build());
        when(cluster.getNodes()).thenReturn(nodes);
        final Result actual = check.check();
        verify(cluster).getNodes();
        assertThat(actual.isHealthy()).isTrue();
    }

    @Test
    public void testCheckUnhealthy() throws Exception {
        final List<RiakNode> nodes = ImmutableList.of();
        when(cluster.getNodes()).thenReturn(nodes);
        final Result actual = check.check();
        verify(cluster).getNodes();
        assertThat(actual.isHealthy()).isFalse();
    }
}
