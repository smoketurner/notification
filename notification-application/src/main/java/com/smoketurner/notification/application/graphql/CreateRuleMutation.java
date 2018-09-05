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
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.dropwizard.util.Duration;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRuleMutation implements DataFetcher<Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateRuleMutation.class);
  private final RuleStore store;

  /**
   * Constructor
   *
   * @param store Rule data store
   */
  public CreateRuleMutation(final RuleStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Override
  public Boolean get(DataFetchingEnvironment environment) {
    final String category = environment.getArgument("category");
    if (Strings.isNullOrEmpty(category)) {
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

    final Map<String, Object> input = environment.getArgument("rule");
    if (input == null || input.isEmpty()) {
      throw new GraphQLValidationError("rule cannot be empty");
    }

    final Rule.Builder builder = Rule.builder();
    if (input.containsKey("maxSize")) {
      try {
        builder.withMaxSize(Integer.parseInt(String.valueOf(input.get("maxSize"))));
      } catch (NumberFormatException e) {
        throw new GraphQLValidationError("maxSize is not an integer");
      }
    }
    if (input.containsKey("maxDuration")) {
      try {
        builder.withMaxDuration(Duration.parse(String.valueOf(input.get("maxDuration"))));
      } catch (IllegalArgumentException e) {
        throw new GraphQLValidationError("maxDuration is an invalid duration");
      }
    }
    if (input.containsKey("matchOn")) {
      builder.withMatchOn(String.valueOf(input.get("matchOn")));
    }

    final Rule rule = builder.build();

    if (!rule.isValid()) {
      throw new GraphQLValidationError("rule cannot be empty");
    }

    try {
      store.store(category, rule);
    } catch (NotificationStoreException e) {
      LOGGER.error(String.format("Unable to create rule for %s", category), e);
      throw new GraphQLValidationError("Unable to create rule");
    }

    return true;
  }
}
