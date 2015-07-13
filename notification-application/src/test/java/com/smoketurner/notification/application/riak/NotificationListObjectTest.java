package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.smoketurner.notification.api.Notification;

public class NotificationListObjectTest {

    private NotificationListObject list;

    @Before
    public void setUp() {
        list = new NotificationListObject();
    }

    @Test
    public void testMaximumNumberOfNotifications() {
        for (long i = 0; i <= 2000; i++) {
            list.addNotification(Notification.newBuilder().withId(i).build());
        }

        final List<Notification> actual = ImmutableList.copyOf(list
                .getNotificationList());
        assertThat(actual).hasSize(1000);
        assertThat(actual.get(0).getId().get()).isEqualTo(2000L);
        assertThat(actual.get(actual.size() - 1).getId().get())
                .isEqualTo(1001L);
    }

    @Test
    public void testNoDuplicateNotifications() {
        for (long i = 0; i < 5; i++) {
            list.addNotification(Notification.newBuilder().withId(1L).build());
        }

        final List<Notification> actual = ImmutableList.copyOf(list
                .getNotificationList());
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getId().get()).isEqualTo(1L);
    }
}
