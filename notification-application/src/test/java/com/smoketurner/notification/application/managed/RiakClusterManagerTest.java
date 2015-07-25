package com.smoketurner.notification.application.managed;

import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.Test;
import com.basho.riak.client.core.RiakCluster;

public class RiakClusterManagerTest {

    private final RiakCluster cluster = mock(RiakCluster.class);
    private final RiakClusterManager manager = new RiakClusterManager(cluster);

    @Test
    public void testNullManager() throws Exception {
        try {
            new RiakClusterManager(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testStart() throws Exception {
        manager.start();
        verify(cluster).start();
    }

    @Test
    public void testStop() throws Exception {
        manager.stop();
        verify(cluster).shutdown();
    }
}
