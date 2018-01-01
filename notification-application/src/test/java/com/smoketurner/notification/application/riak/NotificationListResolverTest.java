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
package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.smoketurner.notification.api.Notification;

@SuppressWarnings("NullAway")
public class NotificationListResolverTest {

    private final NotificationListResolver resolver = new NotificationListResolver();

    @Test
    public void testNoSiblings() throws Exception {
        final List<NotificationListObject> siblings = Collections.emptyList();
        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isNull();
    }

    @Test
    public void testSingleSibling() throws Exception {
        final NotificationListObject list = new NotificationListObject("test");
        final List<NotificationListObject> siblings = Collections
                .singletonList(list);
        final NotificationListObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(list);
    }

    @Test
    public void testSingleSiblingWithDelete() throws Exception {
        final NotificationListObject list = new NotificationListObject("test");
        list.addNotification(createNotification(1L));
        list.deleteNotification(1L);
        final List<NotificationListObject> siblings = Collections
                .singletonList(list);
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

        final List<NotificationListObject> siblings = Arrays.asList(list1,
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
        list3.deleteNotifications(Arrays.asList(3L, 6L));

        final List<NotificationListObject> siblings = Arrays.asList(list1,
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
        final List<Notification> notifications = Lists.newArrayList(
                createNotification(1L), createNotification(2L),
                createNotification(3L), Notification.builder().build());

        final List<Notification> expected = Arrays
                .asList(createNotification(2L), createNotification(3L));

        NotificationListResolver.removeNotifications(notifications,
                Lists.newArrayList(1L));
        assertThat(notifications).containsExactlyElementsOf(expected);
    }

    @Test
    public void testRemoveNotificationsEmpty() throws Exception {
        final List<Notification> notifications = Lists.newArrayList(
                createNotification(1L), createNotification(2L),
                createNotification(3L), Notification.builder().build());

        final List<Notification> expected = Arrays.asList(
                createNotification(1L), createNotification(2L),
                createNotification(3L));

        NotificationListResolver.removeNotifications(notifications,
                Lists.newArrayList());
        assertThat(notifications).containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
