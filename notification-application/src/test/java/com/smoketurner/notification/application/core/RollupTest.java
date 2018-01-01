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
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;
import io.dropwizard.util.Duration;

public class RollupTest {

    @Test
    public void testMatchOn() {
        final Notification n1 = Notification.builder(createNotification(1))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder(createNotification(2))
                .withProperties(ImmutableMap.of("first_name", "John")).build();
        final Notification n3 = Notification.builder(createNotification(3))
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder(createNotification(4))
                .withProperties(ImmutableMap.of("city", "Anytown")).build();
        final Notification n5 = Notification.builder(createNotification(5))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n6 = createNotification(6);
        final Notification n7 = Notification.builder(createNotification(7))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n8 = Notification.builder(createNotification(8))
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n9 = Notification.builder(createNotification(9))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();

        final ImmutableSortedSet<Notification> notifications = ImmutableSortedSet
                .<Notification>naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = Rule.builder().withMatchOn("first_name").build();

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder().fromNotification(n9)
                .withNotifications(Arrays.asList(n7, n5, n1)).build();

        final List<Notification> expected = Arrays.asList(rollup1, n8, n6, n4,
                n3, n2);

        final Stream<Notification> actual = rollup
                .rollup(notifications.stream());
        assertThat(actual.iterator()).containsExactlyElementsOf(expected);
    }

    @Test
    public void testMatchDuration() {
        final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

        final Notification n1 = Notification.builder(createNotification(1))
                .withCreatedAt(now.minusMinutes(40)).build();
        final Notification n2 = Notification.builder(createNotification(2))
                .withCreatedAt(now.minusMinutes(35)).build();
        final Notification n3 = Notification.builder(createNotification(3))
                .withCreatedAt(now.minusMinutes(30)).build();
        final Notification n4 = Notification.builder(createNotification(4))
                .withCreatedAt(now.minusMinutes(25)).build();
        final Notification n5 = Notification.builder(createNotification(5))
                .withCreatedAt(now.minusMinutes(20)).build();
        final Notification n6 = Notification.builder(createNotification(6))
                .withCreatedAt(now.minusMinutes(15)).build();
        final Notification n7 = Notification.builder(createNotification(7))
                .withCreatedAt(now.minusMinutes(10)).build();
        final Notification n8 = Notification.builder(createNotification(8))
                .withCreatedAt(now.minusMinutes(5)).build();
        final Notification n9 = Notification.builder(createNotification(9))
                .withCreatedAt(now).build();

        final ImmutableSortedSet<Notification> notifications = ImmutableSortedSet
                .<Notification>naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = Rule.builder().withMaxSize(3)
                .withMaxDuration(Duration.minutes(20)).build();

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder().fromNotification(n9)
                .withNotifications(Arrays.asList(n8, n7, n6)).build();

        final Notification rollup2 = Notification.builder().fromNotification(n5)
                .withNotifications(Arrays.asList(n4, n3, n2)).build();

        final List<Notification> expected = Arrays.asList(rollup1, rollup2, n1);

        final Stream<Notification> actual = rollup
                .rollup(notifications.stream());
        assertThat(actual.iterator()).containsExactlyElementsOf(expected);
    }

    @Test
    public void testMatchSize() {
        final Notification n1 = createNotification(1);
        final Notification n2 = createNotification(2);
        final Notification n3 = createNotification(3);
        final Notification n4 = createNotification(4);
        final Notification n5 = createNotification(5);
        final Notification n6 = createNotification(6);
        final Notification n7 = createNotification(7);
        final Notification n8 = createNotification(8);
        final Notification n9 = createNotification(9);

        final ImmutableSortedSet<Notification> notifications = ImmutableSortedSet
                .<Notification>naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = Rule.builder().withMaxSize(2).build();

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder().fromNotification(n9)
                .withNotifications(Arrays.asList(n8, n7)).build();

        final Notification rollup2 = Notification.builder().fromNotification(n6)
                .withNotifications(Arrays.asList(n5, n4)).build();

        final Notification rollup3 = Notification.builder().fromNotification(n3)
                .withNotifications(Arrays.asList(n2, n2)).build();

        final List<Notification> expected = Arrays.asList(rollup1, rollup2,
                rollup3);

        final Stream<Notification> actual = rollup
                .rollup(notifications.stream());
        assertThat(actual.iterator()).containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder("new-follower").withId(id).build();
    }
}
