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
import org.junit.Test;
import com.smoketurner.notification.api.Notification;

public class NotificationListAdditionTest {

    @Test
    public void testAddsToNotification() {
        final Notification notification = Notification.builder().withId(1L)
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
        final Notification notification = Notification.builder().withId(1L)
                .build();

        final NotificationListAddition update = new NotificationListAddition(
                notification);

        final NotificationListObject expected = new NotificationListObject();
        expected.addNotification(notification);

        final NotificationListObject actual = update.apply(null);

        assertThat(actual).isEqualTo(expected);
    }
}
