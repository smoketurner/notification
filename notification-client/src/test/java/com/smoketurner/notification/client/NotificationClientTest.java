/**
 * Copyright 2017 Smoke Turner, LLC.
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
package com.smoketurner.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.testing.junit.DropwizardClientRule;

public class NotificationClientTest {

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    @Path("/v1/notifications/{username}")
    public static class NotificationResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public List<Notification> fetch(
                @PathParam("username") String username) {
            return ImmutableList.of(Notification.builder().withId(1L).build());
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Notification store(@PathParam("username") String username,
                Notification notification) {
            return notification;
        }

        @DELETE
        public Response delete(@PathParam("username") String username,
                @QueryParam("ids") String ids) {
            return Response.noContent().build();
        }
    }

    @Path("/ping")
    public static class PingResource {
        @GET
        public String ping() {
            return "pong";
        }
    }

    @Path("/version")
    public static class VersionResource {
        @GET
        public String version() {
            return "1.0.0";
        }
    }

    @ClassRule
    public final static DropwizardClientRule resources = new DropwizardClientRule(
            new NotificationResource(), new PingResource(),
            new VersionResource());

    private final MetricRegistry registry = new MetricRegistry();
    private NotificationClient client;

    @Before
    public void setUp() {
        final ClientConfig config = new ClientConfig();
        config.register(new JacksonMessageBodyProvider(MAPPER));
        client = new NotificationClient(registry,
                ClientBuilder.newClient(config), resources.baseUri());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testFetch() throws Exception {
        final Optional<ImmutableSortedSet<Notification>> actual = client
                .fetch("test");
        assertThat(actual.isPresent()).isTrue();
        final ImmutableSortedSet<Notification> notifications = actual.get();
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notifications.first().getId().isPresent()).isTrue();
    }

    @Test
    public void testFetchNullUsername() throws Exception {
        try {
            client.fetch(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testFetchEmptyUsername() throws Exception {
        try {
            client.fetch("");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStore() throws Exception {
        final Notification expected = Notification.builder().withId(1L).build();
        final Optional<Notification> actual = client.store("test", expected);
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void testStoreNullNotification() throws Exception {
        try {
            client.store("test", null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testStoreNullUsername() throws Exception {
        final Notification expected = Notification.builder().withId(1L).build();
        try {
            client.store(null, expected);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testStoreEmptyUsername() throws Exception {
        final Notification expected = Notification.builder().withId(1L).build();
        try {
            client.store("", expected);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDeleteAll() throws Exception {
        client.delete("test");
    }

    @Test
    public void testDeleteAllNullUsername() throws Exception {
        try {
            client.delete(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testDeleteAllEmptyUsername() throws Exception {
        try {
            client.delete("");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDelete() throws Exception {
        client.delete("test", ImmutableList.of(1L, 2L));
    }

    @Test
    public void testDeleteNullUsername() throws Exception {
        try {
            client.delete(null, ImmutableList.of(1L));
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testDeleteEmptyUsername() throws Exception {
        try {
            client.delete("", ImmutableList.of(1L));
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDeleteNullIds() throws Exception {
        try {
            client.delete("test", null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testDeleteEmptyIds() throws Exception {
        try {
            client.delete("test", ImmutableList.<Long>of());
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testPing() throws Exception {
        assertThat(client.ping()).isTrue();
    }

    @Test
    public void testVersion() throws Exception {
        assertThat(client.version()).isEqualTo("1.0.0");
    }
}
