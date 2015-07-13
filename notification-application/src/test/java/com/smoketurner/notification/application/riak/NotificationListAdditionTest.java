package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import com.smoketurner.notification.api.Notification;

public class NotificationListAdditionTest {

    @Test
    public void testAddsToNotification() {
        final Notification notification = Notification.newBuilder().withId(1L)
                .build();

        final NotificationListAddition update = new NotificationListAddition(
                notification);

        final NotificationListObject original = new NotificationListObject();

        final NotificationListObject expected = new NotificationListObject();
        expected.addNotification(notification);

        final NotificationListObject actual = update.apply(original);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNoOriginal() {
        final Notification notification = Notification.newBuilder().withId(1L)
                .build();

        final NotificationListAddition update = new NotificationListAddition(
                notification);

        final NotificationListObject expected = new NotificationListObject();
        expected.addNotification(notification);

        final NotificationListObject actual = update.apply(null);

        assertThat(actual).isEqualTo(expected);
    }
}
