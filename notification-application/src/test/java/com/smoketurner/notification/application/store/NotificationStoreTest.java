package com.smoketurner.notification.application.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Test;
import com.basho.riak.client.api.RiakClient;
import com.codahale.metrics.MetricRegistry;
import com.ge.snowizard.core.IdWorker;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.smoketurner.notification.api.Notification;

public class NotificationStoreTest {

    private static final String TEST_USER = "test";
    private static final DateTime NOW = new DateTime("2015-07-17T17:43:25Z");
    private final MetricRegistry registry = new MetricRegistry();
    private final RiakClient client = mock(RiakClient.class);
    private final CursorStore cursors = mock(CursorStore.class);
    private final IdWorker snowizard = mock(IdWorker.class);
    private final NotificationStore store = new NotificationStore(registry,
            client, snowizard, cursors) {
        @Override
        public DateTime now() {
            return NOW;
        }
    };

    @Test
    public void testSetSeenState() throws Exception {
        when(cursors.fetch(eq(TEST_USER), anyString())).thenReturn(
                Optional.of(4L));

        final Notification n1 = createNotification(1);
        final Notification n2 = createNotification(2);
        final Notification n3 = createNotification(3);
        final Notification n4 = createNotification(4);
        final Notification n5 = createNotification(5);
        final Notification n6 = createNotification(6);

        final Notification n1Seen = Notification.newBuilder()
                .fromNotification(n1).withUnseen(false).build();
        final Notification n2Seen = Notification.newBuilder()
                .fromNotification(n2).withUnseen(false).build();
        final Notification n3Seen = Notification.newBuilder()
                .fromNotification(n3).withUnseen(false).build();
        final Notification n4Seen = Notification.newBuilder()
                .fromNotification(n4).withUnseen(false).build();
        final Notification n5Unseen = Notification.newBuilder()
                .fromNotification(n5).withUnseen(true).build();
        final Notification n6Unseen = Notification.newBuilder()
                .fromNotification(n6).withUnseen(true).build();

        final List<Notification> notifications = ImmutableList.of(n6, n5, n4,
                n3, n2, n1);

        final List<Notification> expected = ImmutableList.of(n6Unseen,
                n5Unseen, n4Seen, n3Seen, n2Seen, n1Seen);

        final List<Notification> actual = store.setUnseenState(TEST_USER,
                notifications);
        assertThat(actual).containsExactlyElementsOf(expected);
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
            builder.add(Notification.newBuilder().withId(i).withUnseen(true)
                    .build());
        }
        final List<Notification> expectedUnseen = builder.build();

        builder = ImmutableList.builder();
        for (long i = 1; i < 11; i++) {
            builder.add(Notification.newBuilder().withId(i).withUnseen(false)
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
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 1; i < 11; i++) {
            builder.add(createNotification(i));
        }
        builder.add(Notification
                .newBuilder()
                .withId(100L)
                .withNotifications(
                        ImmutableList.of(createNotification(101L),
                                createNotification(102L))).build());
        builder.add(Notification.newBuilder().withId(150L)
                .withNotifications(ImmutableList.<Notification> of()).build());
        builder.add(Notification.newBuilder().build());

        final List<Notification> notifications = builder.build();

        assertThat(store.findNotification(notifications, 1)).isEqualTo(0);
        assertThat(store.findNotification(notifications, 5)).isEqualTo(4);
        assertThat(store.findNotification(notifications, 10)).isEqualTo(9);
        assertThat(store.findNotification(notifications, 12)).isEqualTo(-1);
        assertThat(store.findNotification(notifications, 100)).isEqualTo(10);
        assertThat(store.findNotification(notifications, 101)).isEqualTo(10);
        assertThat(store.findNotification(notifications, 102)).isEqualTo(10);
        assertThat(store.findNotification(notifications, 103)).isEqualTo(-1);
        assertThat(store.findNotification(notifications, 150)).isEqualTo(11);
    }

    private Notification createNotification(final long id) {
        return Notification.newBuilder().withId(id).build();
    }
}
