package com.smoketurner.notification.api;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import io.dropwizard.jackson.Jackson;
import java.util.TreeSet;
import jersey.repackaged.com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificationTest {
    private final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private final Notification notification = Notification
            .newBuilder()
            .withId(12345L)
            .withCategory("new-follower")
            .withMessage("you have a new follower")
            .withCreatedAt(
                    new DateTime("2015-06-29T21:04:12Z", DateTimeZone.UTC))
            .build();

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
    public void categoryCannotBeNull() throws Exception {
        try {
            Notification.newBuilder().withCategory(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void categoryCannotBeEmpty() throws Exception {
        try {
            Notification.newBuilder().withCategory("");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void messageCannotBeNull() throws Exception {
        try {
            Notification.newBuilder().withMessage(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void messageCannotBeEmpty() throws Exception {
        try {
            Notification.newBuilder().withMessage("");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testComparison() throws Exception {
        final Notification notification1 = Notification.newBuilder().withId(1L)
                .build();
        final Notification notification2 = Notification.newBuilder().withId(2L)
                .build();
        final Notification notification3 = Notification.newBuilder().withId(3L)
                .build();

        assertThat(notification1.compareTo(notification1)).isZero();
        assertThat(notification1.compareTo(notification2)).isEqualTo(1);
        assertThat(notification2.compareTo(notification1)).isEqualTo(-1);

        final TreeSet<Notification> notifications = Sets.newTreeSet();
        notifications.add(notification1);
        notifications.add(notification2);
        notifications.add(notification3);

        assertThat(notifications).containsExactly(notification3, notification2,
                notification1);
    }
}
