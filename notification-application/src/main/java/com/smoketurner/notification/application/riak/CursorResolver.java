/**
 * Copyright 2018 Smoke Turner, LLC.
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
package com.smoketurner.notification.application.riak;

import static com.codahale.metrics.MetricRegistry.name;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.cap.ConflictResolver;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class CursorResolver implements ConflictResolver<CursorObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CursorResolver.class);
    private final Histogram siblingCounts;

    /**
     * Constructor
     */
    public CursorResolver() {
        final MetricRegistry registry = SharedMetricRegistries
                .getOrCreate("default");
        this.siblingCounts = registry
                .histogram(name(CursorResolver.class, "sibling-counts"));
    }

    @Nullable
    @Override
    public CursorObject resolve(final List<CursorObject> siblings)
            throws UnresolvedConflictException {
        LOGGER.debug("Found {} siblings", siblings.size());
        siblingCounts.update(siblings.size());
        if (siblings.size() > 1) {
            Collections.sort(siblings);
            return siblings.get(0);
        } else if (siblings.size() == 1) {
            return siblings.get(0);
        } else {
            return null;
        }
    }
}
