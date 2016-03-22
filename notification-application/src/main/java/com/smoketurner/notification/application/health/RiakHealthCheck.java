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
package com.smoketurner.notification.application.health;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.operations.PingOperation;
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
        this.client = Objects.requireNonNull(client);
    }

    @Override
    protected Result check() throws Exception {
        final PingOperation ping = new PingOperation();
        client.getRiakCluster().execute(ping);
        ping.await(1, TimeUnit.SECONDS);

        if (ping.isSuccess()) {
            return Result.healthy("Riak is healthly");
        }
        return Result.unhealthy("Riak is down");
    }
}
