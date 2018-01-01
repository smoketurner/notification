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
package com.smoketurner.notification.api;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.ZonedDateTime;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.dropwizard.jackson.Jackson;

public class NotificationTest {
    private final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private Notification notification;

    @Before
    public void setUp() throws Exception {
        final Notification notification2 = Notification
                .builder("new-follower", "you have a new follower")
                .withId(12346L)
                .withCreatedAt(ZonedDateTime.parse("2015-06-29T21:04:12Z"))
                .withUnseen(true).withProperties(ImmutableMap.of("first_name",
                        "Test 2", "last_name", "User 2"))
                .build();

        notification = Notification
                .builder("new-follower", "you have a new follower")
                .withId(12345L)
                .withCreatedAt(ZonedDateTime.parse("2015-06-29T21:04:12Z"))
                .withUnseen(true)
                .withProperties(ImmutableMap.of("first_name", "Test",
                        "last_name", "User"))
                .withNotifications(ImmutableList.of(notification2)).build();
    }

    @Test
    public void serializesToJSON() throws Exception {
        final String actual = MAPPER.writeValueAsString(notification);
        final String expected = MAPPER.writeValueAsString(MAPPER.readValue(
                fixture("fixtures/notification.json"), Notification.class));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        final Notification actual = MAPPER.readValue(
                fixture("fixtures/notification.json"), Notification.class);
        assertThat(actual).isEqualTo(notification);
    }

    @Test
    public void testGetId() throws Exception {
        final Notification n1 = Notification.builder().build();
        assertThat(n1.getId(5L)).isEqualTo(5L);
        final Notification n2 = Notification.builder().withId(1000L).build();
        assertThat(n2.getId(6L)).isEqualTo(1000L);
    }

    @Test
    public void testToString() throws Exception {
        final ZonedDateTime now = ZonedDateTime
                .parse("2015-08-14T21:25:19.533Z");
        final Notification n1 = Notification.builder("test-category", "")
                .withId(1L).withCreatedAt(now).build();
        assertThat(n1.toString())
                .isEqualTo("Notification{id=Optional[1], idStr=Optional[1],"
                        + " category=test-category, message=,"
                        + " createdAt=2015-08-14T21:25:19.533Z, unseen=Optional.empty,"
                        + " properties={}, notifications=[]}");
    }

    @Test
    public void testBuilder() throws Exception {
        final Notification n1 = Notification.builder().withId(1L).build();
        final Notification n2 = Notification.builder(n1).build();
        assertThat(n1).isEqualTo(n2);
    }

    @Test
    public void testComparison() throws Exception {
        final Notification n1 = Notification.builder().withId(1L).build();
        final Notification n2 = Notification.builder().withId(2L).build();
        final Notification n3 = Notification.builder().withId(3L).build();

        assertThat(n1.compareTo(n2)).isEqualTo(1);
        assertThat(n2.compareTo(n1)).isEqualTo(-1);

        final TreeSet<Notification> notifications = Sets.newTreeSet();
        notifications.add(n1);
        notifications.add(n2);
        notifications.add(n3);

        assertThat(notifications).containsExactly(n3, n2, n1);

        final Notification n1b = Notification.builder().withId(1L)
                .withUnseen(true).build();

        assertThat(n1.compareTo(n1b) == 0).isEqualTo(n1.equals(n1b));
        assertThat(n1.equals(null)).isFalse();
    }

    @Test
    public void testNaturalOrdering() {
        final Notification n1 = Notification.builder("test").withId(1L).build();
        final Notification n2 = Notification.builder("test").withId(2L).build();
        final Notification n3 = Notification.builder("test").withId(1L).build();
        assertThat(n1.equals(n2)).isEqualTo(n1.compareTo(n2) == 0);
        assertThat(n2.equals(n3)).isEqualTo(n2.compareTo(n3) == 0);
        assertThat(n1.equals(n3)).isEqualTo(n1.compareTo(n3) == 0);
    }
}
