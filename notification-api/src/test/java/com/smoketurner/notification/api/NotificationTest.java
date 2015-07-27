package com.smoketurner.notification.api;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.jackson.Jackson;
import java.util.TreeSet;
import jersey.repackaged.com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class NotificationTest {
    private final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private Notification notification;

    @Before
    public void setUp() throws Exception {
        final Notification notification2 = Notification
                .builder()
                .withId(12346L)
                .withCategory("new-follower")
                .withMessage("you have a new follower")
                .withCreatedAt(
                        new DateTime("2015-06-29T21:04:12Z", DateTimeZone.UTC))
                .withUnseen(true)
                .withProperties(
                        ImmutableMap.of("first_name", "Test 2", "last_name",
                                "User 2")).build();

        notification = Notification
                .builder()
                .withId(12345L)
                .withCategory("new-follower")
                .withMessage("you have a new follower")
                .withCreatedAt(
                        new DateTime("2015-06-29T21:04:12Z", DateTimeZone.UTC))
                .withUnseen(true)
                .withProperties(
                        ImmutableMap.of("first_name", "Test", "last_name",
                                "User"))
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
    public void testComparison() throws Exception {
        final Notification n1 = Notification.builder().withId(1L).build();
        final Notification n2 = Notification.builder().withId(2L).build();
        final Notification n3 = Notification.builder().withId(3L).build();

        assertThat(n1.compareTo(n1)).isZero();
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
    }
}
