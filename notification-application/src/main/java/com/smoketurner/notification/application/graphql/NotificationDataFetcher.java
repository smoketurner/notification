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

import com.google.common.base.Strings;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDataFetcher implements DataFetcher<SortedSet<Notification>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDataFetcher.class);
  private final NotificationStore store;

  /**
   * Constructor
   *
   * @param store Notification data store
   */
  public NotificationDataFetcher(final NotificationStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Nullable
  @Override
  public SortedSet<Notification> get(DataFetchingEnvironment environment) {
    final String username = environment.getArgument("username");
    if (Strings.isNullOrEmpty(username)) {
      return null;
    }

    final Optional<UserNotifications> notifications;
    try {
      notifications = store.fetch(username);
    } catch (NotificationStoreException e) {
      LOGGER.error("Unable to fetch notifications", e);
      return null;
    }

    if (!notifications.isPresent()) {
      return null;
    }

    return notifications.get().getNotifications();
  }
}
