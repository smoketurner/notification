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
import com.google.common.collect.ImmutableList;
import graphql.GraphQLError;
import graphql.execution.instrumentation.fieldvalidation.FieldAndArguments;
import graphql.execution.instrumentation.fieldvalidation.FieldValidation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import graphql.language.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsernameFieldValidation implements FieldValidation {

  private static final Logger LOGGER = LoggerFactory.getLogger(UsernameFieldValidation.class);
  private static final List<String> VALID_FIELDS =
      ImmutableList.of(
          "notifications", "createNotification", "removeNotification", "removeAllNotifications");

  private static final int USERNAME_MIN_LENGTH = 3;
  private static final int USERNAME_MAX_LENGTH = 64;

  @Override
  public List<GraphQLError> validateFields(FieldValidationEnvironment environment) {
    final List<GraphQLError> errors = new ArrayList<>();

    for (FieldAndArguments fieldAndArguments : environment.getFields()) {
      final Field field = fieldAndArguments.getField();
      if (!VALID_FIELDS.contains(field.getName())) {
        continue;
      }

      LOGGER.debug("Field: {}", field.getName());

      final String username = fieldAndArguments.getArgumentValue("username");

      if (Strings.isNullOrEmpty(username)) {
        errors.add(environment.mkError("username cannot be empty", fieldAndArguments));
      } else {
        final int length = username.codePointCount(0, username.length());

        if (length < USERNAME_MIN_LENGTH || length > USERNAME_MAX_LENGTH) {
          errors.add(
              environment.mkError(
                  String.format(
                      "username must be between %d and %d characters",
                      USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH),
                  fieldAndArguments));
        } else if (!username.matches("[A-Za-z0-9]+")) {
          errors.add(
              environment.mkError(
                  "username must only contain alphanumeric characters", fieldAndArguments));
        }
      }
    }

    return errors;
  }
}
