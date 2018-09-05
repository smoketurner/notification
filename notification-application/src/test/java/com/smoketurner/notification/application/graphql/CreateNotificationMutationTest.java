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
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.smoketurner.dropwizard.graphql.GraphQLValidationError;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CreateNotificationMutationTest {

  private final NotificationStore store = mock(NotificationStore.class);
  private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
  private final CreateNotificationMutation mutation = new CreateNotificationMutation(store);

  @Test
  public void testUsernameNull() throws Exception {
    when(environment.getArgument("username")).thenReturn(null);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("username cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testUsernameEmpty() throws Exception {
    when(environment.getArgument("username")).thenReturn("");

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("username cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testNotificationNull() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification")).thenReturn(null);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("notification cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testNotificationEmpty() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification")).thenReturn(Collections.emptyMap());

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("notification cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testCategoryNull() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification"))
        .thenReturn(Collections.singletonMap("category", null));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testCategoryEmpty() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification")).thenReturn(ImmutableMap.of("category", ""));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testCategoryTooShort() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification")).thenReturn(ImmutableMap.of("category", "aa"));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category must be between 3 and 20 characters");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testCategoryTooLong() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification"))
        .thenReturn(ImmutableMap.of("category", "thisisareallylongcategory"));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category must be between 3 and 20 characters");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testMessageNull() throws Exception {
    final Map<String, Object> expected = new HashMap<>();
    expected.put("category", "like");
    expected.put("message", null);

    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification")).thenReturn(expected);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("message cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testMessageEmpty() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification"))
        .thenReturn(ImmutableMap.of("category", "like", "message", ""));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("message cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Notification.class));
  }

  @Test
  public void testStoreException() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification"))
        .thenReturn(ImmutableMap.of("category", "like", "message", "this is a test"));
    doThrow(new NotificationStoreException())
        .when(store)
        .store(anyString(), any(Notification.class));

    final Notification notification = Notification.builder("like", "this is a test").build();

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("Unable to create notification");
    }

    verify(store).store(eq("test"), eq(notification));
  }

  @Test
  public void testStoreNotification() throws Exception {
    when(environment.getArgument("username")).thenReturn("test");
    when(environment.getArgument("notification"))
        .thenReturn(ImmutableMap.of("category", "like", "message", "this is a test"));

    final Notification notification = Notification.builder("like", "this is a test").build();
    when(store.store(anyString(), any(Notification.class))).thenReturn(notification);

    final Notification actual = mutation.get(environment);
    verify(store).store(eq("test"), eq(notification));

    assertThat(actual).isNotNull();

    assertThat(actual.getCategory()).isEqualTo(notification.getCategory());
    assertThat(actual.getMessage()).isEqualTo(notification.getMessage());
  }
}
