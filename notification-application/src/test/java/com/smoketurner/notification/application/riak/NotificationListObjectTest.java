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
import java.util.SortedSet;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.smoketurner.notification.api.Notification;

public class NotificationListObjectTest {

    private NotificationListObject list;

    @Before
    public void setUp() {
        list = new NotificationListObject("test");
    }

    @Test
    public void testMaximumNumberOfNotifications() {
        for (long i = 0; i <= 2000; i++) {
            list.addNotification(createNotification(i));
        }

        final SortedSet<Notification> actual = list.getNotifications();
        assertThat(actual).hasSize(1000);
        assertThat(actual.first().getId().get()).isEqualTo(2000L);
        assertThat(actual.last().getId().get()).isEqualTo(1001L);
    }

    @Test
    public void testMaximumNumberOfNotificationsCollection() {
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 0; i <= 2000; i++) {
            builder.add(createNotification(i));
        }

        list.addNotifications(builder.build());

        final SortedSet<Notification> actual = list.getNotifications();
        assertThat(actual).hasSize(1000);
        assertThat(actual.first().getId().get()).isEqualTo(2000L);
        assertThat(actual.last().getId().get()).isEqualTo(1001L);
    }

    @Test
    public void testNoDuplicateNotifications() {
        for (long i = 0; i < 5; i++) {
            list.addNotification(createNotification(1L));
        }

        final SortedSet<Notification> actual = list.getNotifications();
        assertThat(actual).hasSize(1);
        assertThat(actual.first().getId().get()).isEqualTo(1L);
    }

    @Test
    public void testEquals() {
        final NotificationListObject list = new NotificationListObject();
        assertThat(list.equals(null)).isFalse();
    }

    @Test
    public void testDeleteNotification() {
        list.deleteNotification(1L);
        assertThat(list.getDeletedIds()).contains(1L);
    }

    @Test
    public void testGetKey() {
        assertThat(list.getKey()).isEqualTo("test");
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
