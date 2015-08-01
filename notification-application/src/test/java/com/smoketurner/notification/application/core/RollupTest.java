package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;

public class RollupTest {

    @Test
    public void testMatchOn() {
        final Notification n1 = Notification.builder()
                .fromNotification(createNotification(1))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder()
                .fromNotification(createNotification(2))
                .withProperties(ImmutableMap.of("first_name", "John")).build();
        final Notification n3 = Notification.builder()
                .fromNotification(createNotification(3))
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder()
                .fromNotification(createNotification(4))
                .withProperties(ImmutableMap.of("city", "Anytown")).build();
        final Notification n5 = Notification.builder()
                .fromNotification(createNotification(5))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n6 = createNotification(6);
        final Notification n7 = Notification.builder()
                .fromNotification(createNotification(7))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n8 = Notification.builder()
                .fromNotification(createNotification(8))
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n9 = Notification.builder()
                .fromNotification(createNotification(9))
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();

        final ImmutableSortedSet<Notification> notifications = ImmutableSortedSet
                .<Notification> naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = new Rule(Optional.<Integer> absent(),
                Optional.<Duration> absent(), Optional.of("first_name"));

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder()
                .fromNotification(n9)
                .withNotifications(ImmutableList.of(n7, n5, n1)).build();

        final ImmutableList<Notification> expected = ImmutableList.of(rollup1,
                n8, n6, n4, n3, n2);

        final Iterable<Notification> actual = rollup.rollup(notifications);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testMatchDuration() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        final Notification n1 = Notification.builder()
                .fromNotification(createNotification(1))
                .withCreatedAt(now.minusMinutes(40)).build();
        final Notification n2 = Notification.builder()
                .fromNotification(createNotification(2))
                .withCreatedAt(now.minusMinutes(35)).build();
        final Notification n3 = Notification.builder()
                .fromNotification(createNotification(3))
                .withCreatedAt(now.minusMinutes(30)).build();
        final Notification n4 = Notification.builder()
                .fromNotification(createNotification(4))
                .withCreatedAt(now.minusMinutes(25)).build();
        final Notification n5 = Notification.builder()
                .fromNotification(createNotification(5))
                .withCreatedAt(now.minusMinutes(20)).build();
        final Notification n6 = Notification.builder()
                .fromNotification(createNotification(6))
                .withCreatedAt(now.minusMinutes(15)).build();
        final Notification n7 = Notification.builder()
                .fromNotification(createNotification(7))
                .withCreatedAt(now.minusMinutes(10)).build();
        final Notification n8 = Notification.builder()
                .fromNotification(createNotification(8))
                .withCreatedAt(now.minusMinutes(5)).build();
        final Notification n9 = Notification.builder()
                .fromNotification(createNotification(9)).withCreatedAt(now)
                .build();

        final ImmutableSortedSet<Notification> notifications = ImmutableSortedSet
                .<Notification> naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = new Rule(Optional.of(3), Optional.of(Duration
                .minutes(20)), Optional.<String> absent());

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder()
                .fromNotification(n9)
                .withNotifications(ImmutableList.of(n8, n7, n6)).build();

        final Notification rollup2 = Notification.builder()
                .fromNotification(n5)
                .withNotifications(ImmutableList.of(n4, n3, n2)).build();

        final ImmutableList<Notification> expected = ImmutableList.of(rollup1,
                rollup2, n1);

        final Iterable<Notification> actual = rollup.rollup(notifications);
        assertThat(actual).containsExactlyElementsOf(expected);
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
                .<Notification> naturalOrder()
                .add(n1, n2, n3, n4, n5, n6, n7, n8, n9).build();

        final Rule rule = new Rule(Optional.of(2),
                Optional.<Duration> absent(), Optional.<String> absent());

        final Rollup rollup = new Rollup(ImmutableMap.of("new-follower", rule));

        final Notification rollup1 = Notification.builder()
                .fromNotification(n9)
                .withNotifications(ImmutableList.of(n8, n7)).build();

        final Notification rollup2 = Notification.builder()
                .fromNotification(n6)
                .withNotifications(ImmutableList.of(n5, n4)).build();

        final Notification rollup3 = Notification.builder()
                .fromNotification(n3)
                .withNotifications(ImmutableList.of(n2, n2)).build();

        final ImmutableList<Notification> expected = ImmutableList.of(rollup1,
                rollup2, rollup3);

        final Iterable<Notification> actual = rollup.rollup(notifications);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withCategory("new-follower").withId(id)
                .build();
    }
}
