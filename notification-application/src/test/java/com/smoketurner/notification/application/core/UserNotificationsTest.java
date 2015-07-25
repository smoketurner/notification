package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.smoketurner.notification.api.Notification;

public class UserNotificationsTest {

    @Test
    public void testIsEmpty() {
        final UserNotifications notifications = new UserNotifications();
        assertThat(notifications.isEmpty()).isTrue();
        assertThat(notifications.getUnseen()).isEmpty();
        assertThat(notifications.getSeen()).isEmpty();
    }

    @Test
    public void testUnseen() {
        final List<Notification> unseen = ImmutableList.of(Notification
                .builder().withId(1L).build());
        final UserNotifications notifications = new UserNotifications(unseen);
        assertThat(notifications.isEmpty()).isFalse();
        assertThat(notifications.getUnseen()).isEqualTo(unseen);
        assertThat(notifications.getSeen()).isEmpty();
    }

    @Test
    public void testSeenUnseen() {
        final List<Notification> unseen = ImmutableList.of(Notification
                .builder().withId(1L).build());
        final List<Notification> seen = ImmutableList.of(Notification.builder()
                .withId(2L).build());
        final List<Notification> expected = ImmutableList.of(Notification
                .builder().withId(2L).build(), Notification.builder()
                .withId(1L).build());
        final UserNotifications notifications = new UserNotifications(unseen,
                seen);
        assertThat(notifications.isEmpty()).isFalse();
        assertThat(notifications.getUnseen()).containsAll(unseen);
        assertThat(notifications.getSeen()).containsAll(seen);
        assertThat(notifications.getNotifications()).containsExactlyElementsOf(
                expected);
    }
}
