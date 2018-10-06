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
package com.smoketurner.notification.application.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import graphql.schema.DataFetchingEnvironment;

public class NotificationDataFetcherTest {

  private final NotificationStore store = mock(NotificationStore.class);
  private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
  private final NotificationDataFetcher fetcher = new NotificationDataFetcher(store);

  @Test
  public void testUsernameNull() throws Exception {
    when(environment.getArgument("username")).thenReturn(null);

    final SortedSet<Notification> actual = fetcher.get(environment);

    assertThat(actual).isNull();

    verify(store, never()).fetch(anyString());
  }

  @Test
  public void testUsernameEmpty() throws Exception {
    when(environment.getArgument("username")).thenReturn("");

    final SortedSet<Notification> actual = fetcher.get(environment);

    assertThat(actual).isNull();

    verify(store, never()).fetch(anyString());
  }

  @Test
  public void testStoreException() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    doThrow(new NotificationStoreException()).when(store).fetch(anyString());

    final SortedSet<Notification> actual = fetcher.get(environment);

    assertThat(actual).isNull();

    verify(store).fetch(eq("test"));
  }

  @Test
  public void testNoNotifications() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");

    when(store.fetch(anyString())).thenReturn(Optional.empty());

    final SortedSet<Notification> actual = fetcher.get(environment);
    verify(store).fetch(eq("test"));

    assertThat(actual).isNotNull();
    assertThat(actual).isEmpty();
  }

  @Test
  public void testFetchNotifications() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");

    final Notification n1 = Notification.create("1");
    final Notification n2 = Notification.create("2");
    final Notification n3 = Notification.create("3");
    final Notification n4 = Notification.create("4");

    final TreeSet<Notification> set = new TreeSet<>();
    set.add(n1);
    set.add(n2);
    set.add(n3);
    set.add(n4);

    final UserNotifications notifications = new UserNotifications(set);

    when(store.fetch(anyString())).thenReturn(Optional.of(notifications));

    final SortedSet<Notification> actual = fetcher.get(environment);
    verify(store).fetch(eq("test"));

    assertThat(actual).isNotNull();
    assertThat(actual.first()).isEqualTo(n4);
    assertThat(actual.last()).isEqualTo(n1);
    assertThat(actual.size()).isEqualTo(4);
  }
}
