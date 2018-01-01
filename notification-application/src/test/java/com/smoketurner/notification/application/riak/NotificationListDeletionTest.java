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
