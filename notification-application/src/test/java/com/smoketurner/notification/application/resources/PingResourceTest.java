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
package com.smoketurner.notification.application.resources;

import static org.assertj.core.api.Assertions.assertThat;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import io.dropwizard.testing.junit.ResourceTestRule;

public class PingResourceTest {

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new PingResource()).build();

    @Test
    public void testGetPing() throws Exception {
        final Response response = resources.client().target("/ping").request()
                .get();
        final String actual = response.readEntity(String.class);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(actual).isEqualTo("pong");
    }
}
