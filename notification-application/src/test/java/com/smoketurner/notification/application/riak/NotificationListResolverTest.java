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
    public void testSingleSiblingWithDelete() throws Exception {
        final NotificationListObject list = new NotificationListObject("test");
        list.addNotification(createNotification(1L));
        list.deleteNotification(1L);
        final List<NotificationListObject> siblings = ImmutableList.of(list);
        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual.getDeletedIds()).isEmpty();
        assertThat(actual.getNotifications()).isEmpty();
    }

    @Test
    public void testMultipleSibling() throws Exception {
        final Notification n1 = createNotification(1L);
        final Notification n2 = createNotification(2L);
        final Notification n3 = createNotification(3L);
        final Notification n4 = createNotification(4L);
        final Notification n5 = createNotification(5L);
        final Notification n6 = createNotification(6L);

        final NotificationListObject list1 = new NotificationListObject("test");
        list1.addNotification(n1);
        list1.addNotification(n4);
        list1.addNotification(n2);
        list1.addNotification(n3);

        final NotificationListObject list2 = new NotificationListObject("test");
        list2.addNotification(n2);
        list2.addNotification(n3);
        list2.addNotification(n5);

        final NotificationListObject list3 = new NotificationListObject("test");
        list3.addNotification(n6);
        list3.addNotification(n2);
        list3.addNotification(n5);

        final List<NotificationListObject> siblings = ImmutableList.of(list1,
                list2, list3);

        final NotificationListObject expected = new NotificationListObject(
                "test");
        expected.addNotification(n1);
        expected.addNotification(n2);
        expected.addNotification(n3);
        expected.addNotification(n4);
        expected.addNotification(n5);
        expected.addNotification(n6);

        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getNotifications()).containsExactly(n6, n5, n4, n3,
                n2, n1);
        assertThat(actual.getDeletedIds()).isEmpty();
    }

    @Test
    public void testMultipleSiblingWithDeletes() throws Exception {
        final Notification n1 = createNotification(1L);
        final Notification n2 = createNotification(2L);
        final Notification n3 = createNotification(3L);
        final Notification n4 = createNotification(4L);
        final Notification n5 = createNotification(5L);
        final Notification n6 = createNotification(6L);

        final NotificationListObject list1 = new NotificationListObject("test");
        list1.addNotification(n1);
        list1.addNotification(n4);
        list1.addNotification(n2);
        list1.addNotification(n3);
        list1.deleteNotification(3L);

        final NotificationListObject list2 = new NotificationListObject("test");
        list2.addNotification(n2);
        list2.addNotification(n3);
        list2.addNotification(n5);
        list2.deleteNotification(4L);

        final NotificationListObject list3 = new NotificationListObject("test");
        list3.addNotification(n6);
        list3.addNotification(n2);
        list3.addNotification(n5);
        list3.deleteNotifications(ImmutableList.of(3L, 6L));

        final List<NotificationListObject> siblings = ImmutableList.of(list1,
                list2, list3);

        final NotificationListObject expected = new NotificationListObject(
                "test");
        expected.addNotification(n1);
        expected.addNotification(n2);
        expected.addNotification(n5);

        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getNotifications()).containsExactly(n5, n2, n1);
        assertThat(actual.getDeletedIds()).isEmpty();
    }

    @Test
    public void testRemoveNotifications() throws Exception {
        final List<Notification> notifications = ImmutableList.of(
                createNotification(1L), createNotification(2L),
                createNotification(3L), Notification.builder().build());

        final List<Notification> expected = ImmutableList.of(
                createNotification(2L), createNotification(3L));

        final List<Notification> actual = NotificationListResolver
                .removeNotifications(notifications, ImmutableList.of(1L));
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testRemoveNotificationsEmpty() throws Exception {
        final List<Notification> notifications = ImmutableList.of(
                createNotification(1L), createNotification(2L),
                createNotification(3L), Notification.builder().build());

        final List<Notification> expected = ImmutableList.of(
                createNotification(1L), createNotification(2L),
                createNotification(3L));

        final List<Notification> actual = NotificationListResolver
                .removeNotifications(notifications, ImmutableList.<Long> of());
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
