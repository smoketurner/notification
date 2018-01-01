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
package com.smoketurner.notification.application.integration;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.TreeSet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.NotificationApplication;
import com.smoketurner.notification.application.config.NotificationConfiguration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class NotificationsIT {

    private static final String TEST_USER = "test";

    @ClassRule
    public static final DropwizardAppRule<NotificationConfiguration> RULE = new DropwizardAppRule<NotificationConfiguration>(
            NotificationApplication.class,
            ResourceHelpers.resourceFilePath("notification-test.yml"));

    private static Client client;

    @BeforeClass
    public static void setUp() throws Exception {
        client = new JerseyClientBuilder(RULE.getEnvironment())
                .build("test client");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testCreateNotification() throws Exception {
        final Notification notification = createNotification();

        final Response response = client.target(getUrl()).request()
                .post(Entity.json(notification));

        final Notification actual = response.readEntity(Notification.class);

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getLocation().getPath())
                .isEqualTo("/v1/notifications/" + TEST_USER);
        assertThat(actual.getCategory()).isEqualTo(notification.getCategory());
        assertThat(actual.getMessage()).isEqualTo(notification.getMessage());
        assertThat(actual.getId().isPresent()).isTrue();
        assertThat(actual.getCreatedAt()).isNotNull();
    }

    @Test
    public void testPagination() throws Exception {
        testDeleteAllNotifications();

        final TreeSet<Notification> expected = Sets.newTreeSet();
        for (int i = 0; i < 100; i++) {
            final Notification response = client.target(getUrl()).request()
                    .post(Entity.json(createNotification()),
                            Notification.class);
            expected.add(
                    Notification.builder(response).withUnseen(true).build());
        }

        int requests = 0;
        String nextRange = null;
        final ImmutableList.Builder<Notification> results = ImmutableList
                .builder();
        boolean paginate = true;
        while (paginate) {
            final Invocation.Builder builder = client.target(getUrl())
                    .request();
            if (nextRange != null) {
                builder.header("Range", nextRange);
            }

            final Response response = builder.get();
            nextRange = response.getHeaderString("Next-Range");
            if (nextRange == null) {
                paginate = false;
            }

            if (response.getStatus() == 200 || response.getStatus() == 206) {
                final List<Notification> list = response
                        .readEntity(new GenericType<List<Notification>>() {
                        });
                assertThat(list.size()).isEqualTo(20);
                results.addAll(list);
            }

            requests++;
        }

        final List<Notification> actual = results.build();
        assertThat(actual).containsExactlyElementsOf(expected);
        assertThat(requests).isEqualTo(5);

        testDeleteAllNotifications();
    }

    @Test
    public void testFetchNotifications() {
        final Response response = client.target(getUrl()).request().get();

        if (response.getStatus() != 404) {
            assertThat(response.getStatus()).isEqualTo(200);
            final List<Notification> actual = response
                    .readEntity(new GenericType<List<Notification>>() {
                    });
            assertThat(actual.size()).isGreaterThan(0);
            final Notification first = actual.get(0);
            assertThat(first.getId().isPresent()).isTrue();
            assertThat(first.getCreatedAt()).isNotNull();
        }
    }

    @Test
    public void testDeleteAllNotifications() {
        final Response response = client.target(getUrl()).request().delete();
        assertThat(response.getStatus()).isEqualTo(204);
    }

    private static String getUrl() {
        return String.format("http://127.0.0.1:%d/v1/notifications/%s",
                RULE.getLocalPort(), TEST_USER);
    }

    private static Notification createNotification() {
        return Notification.builder("test-category", "this is only a test")
                .build();
    }
}
