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
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateNotificationMutation implements DataFetcher<Notification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateNotificationMutation.class);
  private final NotificationStore store;

  /**
   * Constructor
   *
   * @param store Notification data store
   */
  public CreateNotificationMutation(final NotificationStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Override
  public Notification get(DataFetchingEnvironment environment) {
    final String username = environment.getArgument("username");
    if (Strings.isNullOrEmpty(username)) {
      throw new GraphQLValidationError("username cannot be empty");
    }

    final Map<String, Object> input = environment.getArgument("notification");
    if (input == null || input.isEmpty()) {
      throw new GraphQLValidationError("notification cannot be empty");
    }

    final String category = String.valueOf(input.get("category")).trim();
    if (Strings.isNullOrEmpty(category) || "null".equals(category)) {
      throw new GraphQLValidationError("category cannot be empty");
    }

    final int categoryLength = category.codePointCount(0, category.length());

    if (categoryLength < Notification.CATEGORY_MIN_LENGTH
        || categoryLength > Notification.CATEGORY_MAX_LENGTH) {
      throw new GraphQLValidationError(
          String.format(
              "category must be between %d and %d characters",
              Notification.CATEGORY_MIN_LENGTH, Notification.CATEGORY_MAX_LENGTH));
    }

    final String message = String.valueOf(input.get("message")).trim();
    if (Strings.isNullOrEmpty(message) || "null".equals(message)) {
      throw new GraphQLValidationError("message cannot be empty");
    }

    final int messageLength = message.codePointCount(0, message.length());

    if (messageLength < Notification.MESSAGE_MIN_LENGTH
        || messageLength > Notification.MESSAGE_MAX_LENGTH) {
      throw new GraphQLValidationError(
          String.format(
              "message must be between %d and %d characters",
              Notification.MESSAGE_MIN_LENGTH, Notification.MESSAGE_MAX_LENGTH));
    }

    final Notification.Builder builder = Notification.builder(category, message);

    final Object properties = input.get("properties");
    if (properties != null && properties instanceof Map) {
      builder.withProperties(convertToMap(properties));
    }

    final Notification notification = builder.build();

    try {
      return store.store(username, notification);
    } catch (NotificationStoreException e) {
      LOGGER.error(String.format("Unable to create notification for %s", username), e);
      throw new GraphQLValidationError("Unable to create notification");
    }
  }

  /**
   * Safely convert an Object into a Map of string objects.
   *
   * @param obj Object to convert
   * @return Map of properties
   */
  private static Map<String, String> convertToMap(final Object obj) {
    if (obj == null) {
      return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) obj;
    if (map == null || map.isEmpty()) {
      return Collections.emptyMap();
    }

    return map.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()).trim()));
  }
}
