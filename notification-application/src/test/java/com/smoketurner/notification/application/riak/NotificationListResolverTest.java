package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.smoketurner.notification.api.Notification;

public class NotificationListResolverTest {

    private final NotificationListResolver resolver = new NotificationListResolver();

    @Test
    public void testNoSiblings() throws Exception {
        final List<NotificationListObject> siblings = ImmutableList.of();
        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isNull();
    }

    @Test
    public void testSingleSibling() throws Exception {
        final NotificationListObject list = new NotificationListObject("test");
        final List<NotificationListObject> siblings = ImmutableList.of(list);
        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(list);
    }

    @Test
    public void testMultipleSibling() throws Exception {
        final Notification notification1 = Notification.builder().withId(1L)
                .build();
        final Notification notification2 = Notification.builder().withId(2L)
                .build();
        final Notification notification3 = Notification.builder().withId(3L)
                .build();
        final Notification notification4 = Notification.builder().withId(4L)
                .build();
        final Notification notification5 = Notification.builder().withId(5L)
                .build();
        final Notification notification6 = Notification.builder().withId(6L)
                .build();

        final NotificationListObject list1 = new NotificationListObject("test");
        list1.addNotification(notification1);
        list1.addNotification(notification4);
        list1.addNotification(notification2);
        list1.addNotification(notification3);

        final NotificationListObject list2 = new NotificationListObject("test");
        list1.addNotification(notification2);
        list1.addNotification(notification3);
        list1.addNotification(notification5);

        final NotificationListObject list3 = new NotificationListObject("test");
        list1.addNotification(notification6);
        list1.addNotification(notification2);
        list1.addNotification(notification5);

        final List<NotificationListObject> siblings = ImmutableList.of(list1,
                list2, list3);

        final NotificationListObject expected = new NotificationListObject(
                "test");
        expected.addNotification(notification1);
        expected.addNotification(notification2);
        expected.addNotification(notification3);
        expected.addNotification(notification4);
        expected.addNotification(notification5);
        expected.addNotification(notification6);

        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getNotifications()).containsExactly(notification6,
                notification5, notification4, notification3, notification2,
                notification1);
    }
}
