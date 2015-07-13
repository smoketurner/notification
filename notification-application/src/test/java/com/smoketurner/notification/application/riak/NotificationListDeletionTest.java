package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

public class NotificationListDeletionTest {

    @Test
    public void testDeletesFromNotification() {
        final ImmutableList<Long> ids = ImmutableList.of(1L, 2L, 3L);
        final NotificationListDeletion update = new NotificationListDeletion(
                ids);

        final NotificationListObject original = new NotificationListObject();

        final NotificationListObject expected = new NotificationListObject();
        expected.deleteNotifications(ids);

        final NotificationListObject actual = update.apply(original);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNoOriginal() {
        final ImmutableList<Long> ids = ImmutableList.of(1L, 2L, 3L);
        final NotificationListDeletion update = new NotificationListDeletion(
                ids);

        final NotificationListObject expected = new NotificationListObject();
        expected.deleteNotifications(ids);

        final NotificationListObject actual = update.apply(null);

        assertThat(actual).isEqualTo(expected);
    }
}
