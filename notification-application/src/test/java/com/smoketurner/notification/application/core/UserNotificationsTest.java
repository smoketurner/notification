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
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
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
        final List<Notification> unseen = Collections
                .singletonList(createNotification(1L));
        final UserNotifications notifications = new UserNotifications(unseen);
        assertThat(notifications.isEmpty()).isFalse();
        assertThat(notifications.getUnseen()).containsExactlyElementsOf(unseen);
        assertThat(notifications.getSeen()).isEmpty();
    }

    @Test
    public void testSeenUnseen() {
        final List<Notification> unseen = Collections
                .singletonList(createNotification(1L));
        final List<Notification> seen = Collections
                .singletonList(createNotification(2L));
        final List<Notification> expected = Arrays
                .asList(createNotification(2L), createNotification(1L));
        final UserNotifications notifications = new UserNotifications(unseen,
                seen);
        assertThat(notifications.isEmpty()).isFalse();
        assertThat(notifications.getUnseen()).containsExactlyElementsOf(unseen);
        assertThat(notifications.getSeen()).containsExactlyElementsOf(seen);
        assertThat(notifications.getNotifications())
                .containsExactlyElementsOf(expected);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
