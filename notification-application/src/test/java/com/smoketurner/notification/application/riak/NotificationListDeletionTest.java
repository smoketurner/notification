/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class NotificationListDeletionTest {

  @Test
  public void testDeletesFromNotification() {
    final ImmutableList<String> ids = ImmutableList.of("1", "2", "3");
    final NotificationListDeletion update = new NotificationListDeletion(ids);

    final NotificationListObject original = new NotificationListObject();

    final NotificationListObject expected = new NotificationListObject();
    expected.deleteNotifications(ids);

    final NotificationListObject actual = update.apply(original);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testNoOriginal() {
    final ImmutableList<String> ids = ImmutableList.of("1", "2", "3");
    final NotificationListDeletion update = new NotificationListDeletion(ids);

    final NotificationListObject expected = new NotificationListObject();
    expected.deleteNotifications(ids);

    final NotificationListObject actual = update.apply(null);

    assertThat(actual).isEqualTo(expected);
  }
}
