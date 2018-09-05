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
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.smoketurner.notification.api.Notification;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

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
    final List<Notification> unseen = Collections.singletonList(Notification.create("1"));
    final UserNotifications notifications = new UserNotifications(unseen);
    assertThat(notifications.isEmpty()).isFalse();
    assertThat(notifications.getUnseen()).containsExactlyElementsOf(unseen);
    assertThat(notifications.getSeen()).isEmpty();
  }

  @Test
  public void testSeenUnseen() {
    final List<Notification> unseen = Collections.singletonList(Notification.create("2"));
    final List<Notification> seen = Collections.singletonList(Notification.create("1"));
    final List<Notification> expected =
        Arrays.asList(Notification.create("2"), Notification.create("1"));
    final UserNotifications notifications = new UserNotifications(unseen, seen);
    assertThat(notifications.isEmpty()).isFalse();
    assertThat(notifications.getUnseen()).containsExactlyElementsOf(unseen);
    assertThat(notifications.getSeen()).containsExactlyElementsOf(seen);
    assertThat(notifications.getNotifications()).containsExactlyElementsOf(expected);
  }
}
