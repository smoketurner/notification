/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.joda.time.DateTime;
import org.junit.Test;
import com.basho.riak.client.api.RiakClient;
import com.codahale.metrics.MetricRegistry;
import com.ge.snowizard.core.IdWorker;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.Rule;
import com.smoketurner.notification.application.core.UserNotifications;

public class NotificationStoreTest {

    private static final String TEST_USER = "test";
    private static final DateTime NOW = new DateTime("2015-07-17T17:43:25Z");
    private final MetricRegistry registry = new MetricRegistry();
    private final RiakClient client = mock(RiakClient.class);
    private final CursorStore cursors = mock(CursorStore.class);
    private final IdWorker snowizard = mock(IdWorker.class);
    private final Map<String, Rule> rules = ImmutableMap.of();
    private final NotificationStore store = new NotificationStore(registry,
            client, snowizard, cursors, rules) {
        @Override
        public DateTime now() {
            return NOW;
        }
    };

    @Test
    public void testSplitNotifications() throws Exception {
        when(cursors.fetch(TEST_USER, NotificationStore.CURSOR_NAME))
                .thenReturn(Optional.of(4L));

        final Notification n1 = createNotification(1);
        final Notification n2 = createNotification(2);
        final Notification n3 = createNotification(3);
        final Notification n4 = createNotification(4);
        final Notification n5 = createNotification(5);
        final Notification n6 = createNotification(6);

        final Notification n1Seen = Notification.builder().fromNotification(n1)
                .withUnseen(false).build();
        final Notification n2Seen = Notification.builder().fromNotification(n2)
                .withUnseen(false).build();
        final Notification n3Seen = Notification.builder().fromNotification(n3)
                .withUnseen(false).build();
        final Notification n4Seen = Notification.builder().fromNotification(n4)
                .withUnseen(false).build();
        final Notification n5Unseen = Notification.builder()
                .fromNotification(n5).withUnseen(true).build();
        final Notification n6Unseen = Notification.builder()
                .fromNotification(n6).withUnseen(true).build();

        final TreeSet<Notification> notifications = Sets
                .newTreeSet(ImmutableList.of(n6, n5, n4, n3, n2, n1));

        final List<Notification> expected = ImmutableList.of(n6Unseen,
                n5Unseen, n4Seen, n3Seen, n2Seen, n1Seen);

        final UserNotifications actual = store.splitNotifications(TEST_USER,
                notifications);
        verify(cursors).fetch(TEST_USER, NotificationStore.CURSOR_NAME);
        assertThat(actual.getNotifications()).containsExactlyElementsOf(
                expected);
        assertThat(actual.getUnseen()).containsExactly(n6Unseen, n5Unseen);
        assertThat(actual.getSeen()).containsExactly(n4Seen, n3Seen, n2Seen,
                n1Seen);
    }

    @Test
    public void testSplitNotificationsFirst() throws Exception {
        when(cursors.fetch(TEST_USER, NotificationStore.CURSOR_NAME))
                .thenReturn(Optional.of(0L));

        final Notification n1 = createNotification(1);
        final Notification n2 = createNotification(2);
        final Notification n3 = createNotification(3);
        final Notification n4 = createNotification(4);
        final Notification n5 = createNotification(5);
        final Notification n6 = createNotification(6);

        final Notification n1Seen = Notification.builder().fromNotification(n1)
                .withUnseen(true).build();
        final Notification n2Seen = Notification.builder().fromNotification(n2)
                .withUnseen(true).build();
        final Notification n3Seen = Notification.builder().fromNotification(n3)
                .withUnseen(true).build();
        final Notification n4Seen = Notification.builder().fromNotification(n4)
                .withUnseen(true).build();
        final Notification n5Seen = Notification.builder().fromNotification(n5)
                .withUnseen(true).build();
        final Notification n6Seen = Notification.builder().fromNotification(n6)
                .withUnseen(true).build();

        final TreeSet<Notification> notifications = Sets
                .newTreeSet(ImmutableList.of(n6, n5, n4, n3, n2, n1));

        final List<Notification> expected = ImmutableList.of(n6Seen, n5Seen,
                n4Seen, n3Seen, n2Seen, n1Seen);

        final UserNotifications actual = store.splitNotifications(TEST_USER,
                notifications);
        verify(cursors).fetch(TEST_USER, NotificationStore.CURSOR_NAME);
        verify(cursors).store(TEST_USER, NotificationStore.CURSOR_NAME, 6L);
        assertThat(actual.getNotifications()).containsExactlyElementsOf(
                expected);
        assertThat(actual.getUnseen()).containsExactly(n6Seen, n5Seen, n4Seen,
                n3Seen, n2Seen, n1Seen);
        assertThat(actual.getSeen()).isEmpty();
    }

    @Test
    public void testSplitNotificationsNoCursor() throws Exception {
        when(cursors.fetch(TEST_USER, NotificationStore.CURSOR_NAME))
                .thenReturn(Optional.<Long> absent());

        final Notification n1 = createNotification(1);

        final Notification n1Unseen = Notification.builder()
                .fromNotification(n1).withUnseen(true).build();

        final TreeSet<Notification> notifications = Sets
                .newTreeSet(ImmutableList.of(n1));
        final List<Notification> expected = ImmutableList.of(n1Unseen);

        final UserNotifications actual = store.splitNotifications(TEST_USER,
                notifications);
        verify(cursors).fetch(TEST_USER, NotificationStore.CURSOR_NAME);
        verify(cursors).store(TEST_USER, NotificationStore.CURSOR_NAME, 1L);
        assertThat(actual.getNotifications()).containsExactlyElementsOf(
                expected);
    }

    @Test
    public void testSplitNotificationsUserNull() throws Exception {
        try {
            store.splitNotifications(null, Sets.<Notification> newTreeSet());
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testSplitNotificationsUserEmpty() throws Exception {
        try {
            store.splitNotifications("", Sets.<Notification> newTreeSet());
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testSplitNotificationsNotificationsNull() throws Exception {
        try {
            store.splitNotifications(TEST_USER, null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testSplitNotificationsNotificationsEmpty() throws Exception {
        final Set<Notification> expected = ImmutableSortedSet.of();
        final UserNotifications actual = store.splitNotifications(TEST_USER,
                Sets.<Notification> newTreeSet());
        assertThat(actual.getNotifications()).isEqualTo(expected);
    }

    @Test
    public void testSetUnseenState() throws Exception {
        ImmutableList.Builder<Notification> builder = ImmutableList.builder();
        for (long i = 1; i < 11; i++) {
            builder.add(createNotification(i));
        }
        final List<Notification> notifications = builder.build();

        builder = ImmutableList.builder();
        for (long i = 1; i < 11; i++) {
            builder.add(Notification.builder().withId(i).withUnseen(true)
                    .build());
        }
        final List<Notification> expectedUnseen = builder.build();

        builder = ImmutableList.builder();
        for (long i = 1; i < 11; i++) {
            builder.add(Notification.builder().withId(i).withUnseen(false)
                    .build());
        }
        final List<Notification> expectedSeen = builder.build();

        assertThat(store.setUnseenState(notifications, true))
                .containsExactlyElementsOf(expectedUnseen);
        assertThat(store.setUnseenState(notifications, false))
                .containsExactlyElementsOf(expectedSeen);
    }

    @Test
    public void testFindNotification() throws Exception {
        final Notification n100 = Notification
                .builder()
                .withId(100L)
                .withNotifications(
                        ImmutableList.of(createNotification(101L),
                                createNotification(102L))).build();

        final Notification n150 = Notification.builder().withId(150L)
                .withNotifications(ImmutableList.<Notification> of()).build();

        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 1; i < 11; i++) {
            builder.add(createNotification(i));
        }
        builder.add(n100);
        builder.add(n150);
        builder.add(Notification.builder().build());

        final List<Notification> notifications = builder.build();

        assertThat(store.tryFind(notifications, 1)).isEqualTo(
                Optional.of(createNotification(1)));
        assertThat(store.tryFind(notifications, 5)).isEqualTo(
                Optional.of(createNotification(5)));
        assertThat(store.tryFind(notifications, 10)).isEqualTo(
                Optional.of(createNotification(10)));
        assertThat(store.tryFind(notifications, 12)).isEqualTo(
                Optional.<Notification> absent());
        assertThat(store.tryFind(notifications, 100)).isEqualTo(
                Optional.of(n100));
        assertThat(store.tryFind(notifications, 101)).isEqualTo(
                Optional.of(n100));
        assertThat(store.tryFind(notifications, 102)).isEqualTo(
                Optional.of(n100));
        assertThat(store.tryFind(notifications, 103)).isEqualTo(
                Optional.<Notification> absent());
        assertThat(store.tryFind(notifications, 150)).isEqualTo(
                Optional.of(n150));
    }

    @Test
    public void testSkip() throws Exception {
        final ImmutableSortedSet.Builder<Notification> builder = ImmutableSortedSet
                .<Notification> naturalOrder();
        for (long i = 1; i <= 100; i++) {
            builder.add(createNotification(i));
        }
        final ImmutableSortedSet<Notification> notifications = builder.build();

        final ImmutableList.Builder<Notification> builder2 = ImmutableList
                .<Notification> builder();
        for (long i = 100; i > 90; i--) {
            builder2.add(createNotification(i));
        }
        final List<Notification> expected = builder2.build();

        final Iterable<Notification> actual = store.skip(notifications, 0,
                true, 10);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testSkipWithStart() throws Exception {
        final ImmutableSortedSet.Builder<Notification> builder = ImmutableSortedSet
                .<Notification> naturalOrder();
        for (long i = 1; i <= 100; i++) {
            builder.add(createNotification(i));
        }
        final ImmutableSortedSet<Notification> notifications = builder.build();

        final ImmutableList.Builder<Notification> builder2 = ImmutableList
                .<Notification> builder();
        for (long i = 55; i > 45; i--) {
            builder2.add(createNotification(i));
        }
        final List<Notification> expected = builder2.build();

        final Iterable<Notification> actual = store.skip(notifications, 56,
                false, 10);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testSkipWithStartNotFound() throws Exception {
        final ImmutableSortedSet.Builder<Notification> builder = ImmutableSortedSet
                .<Notification> naturalOrder();
        for (long i = 1; i <= 100; i++) {
            builder.add(createNotification(i));
        }
        final ImmutableSortedSet<Notification> notifications = builder.build();

        final ImmutableList.Builder<Notification> builder2 = ImmutableList
                .<Notification> builder();
        for (long i = 100; i > 90; i--) {
            builder2.add(createNotification(i));
        }
        final List<Notification> expected = builder2.build();

        final Iterable<Notification> actual = store.skip(notifications, 1000,
                true, 10);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
