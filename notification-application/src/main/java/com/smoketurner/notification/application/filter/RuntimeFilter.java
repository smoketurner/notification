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
package com.smoketurner.notification.application.filter;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import io.dropwizard.util.Duration;

/**
 * This class adds an "X-Runtime" HTTP response header that includes the time
 * taken to execute the request, in seconds.
 * 
 * @see https://github.com/rack/rack/blob/master/lib/rack/runtime.rb
 */
@PreMatching
public class RuntimeFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final float NANOS_IN_SECOND = Duration.seconds(1)
            .toNanoseconds();
    private static final String RUNTIME_HEADER = "X-Runtime";
    private static final String RUNTIME_PROPERTY = "com.smoketurner.notification.runtime";

    @Override
    public void filter(final ContainerRequestContext request)
            throws IOException {
        request.setProperty(RUNTIME_PROPERTY, System.nanoTime());
    }

    @Override
    public void filter(final ContainerRequestContext request,
            final ContainerResponseContext response) throws IOException {
        final Long startTime = (Long) request.getProperty(RUNTIME_PROPERTY);
        if (startTime != null) {
            final float deltaSeconds = (System.nanoTime() - startTime)
                    / NANOS_IN_SECOND;
            response.getHeaders().add(RUNTIME_HEADER,
                    String.format("%.6f", deltaSeconds));
        }
    }
}
