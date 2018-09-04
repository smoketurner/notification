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
import com.smoketurner.dropwizard.graphql.GraphQLValidationError;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveAllNotificationsMutation implements DataFetcher<Boolean> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RemoveAllNotificationsMutation.class);
  private final NotificationStore store;

  /**
   * Constructor
   *
   * @param store Notification data store
   */
  public RemoveAllNotificationsMutation(final NotificationStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Override
  public Boolean get(DataFetchingEnvironment environment) {
    final String username = environment.getArgument("username");
    if (Strings.isNullOrEmpty(username)) {
      throw new GraphQLValidationError("username cannot be empty");
    }

    try {
      store.removeAll(username);
    } catch (NotificationStoreException e) {
      LOGGER.error(String.format("Unable to remove all notifications for %s", username), e);
      throw new GraphQLValidationError("Unable to remove all notifications");
    }

    return true;
  }
}
