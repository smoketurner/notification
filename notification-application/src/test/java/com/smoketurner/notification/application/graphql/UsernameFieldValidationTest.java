/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.instrumentation.fieldvalidation.FieldAndArguments;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import graphql.language.Field;
import graphql.language.SourceLocation;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;

public class UsernameFieldValidationTest {

  private final UsernameFieldValidation validation = new UsernameFieldValidation();
  private final Field field = mock(Field.class);
  private final FieldAndArguments fieldAndArguments = mock(FieldAndArguments.class);
  private final FieldValidationEnvironment environment = mock(FieldValidationEnvironment.class);

  @Test
  public void testUsernameNull() {
    when(field.getName()).thenReturn("createNotification");
    when(fieldAndArguments.getField()).thenReturn(field);
    when(fieldAndArguments.getArgumentValue("username")).thenReturn(null);
    when(environment.getFields()).thenReturn(Collections.singletonList(fieldAndArguments));
    when(environment.mkError(eq("username cannot be empty"), any(FieldAndArguments.class)))
        .thenReturn(buildError("username cannot be empty"));

    final List<GraphQLError> actual = validation.validateFields(environment);

    verify(environment).mkError(eq("username cannot be empty"), any(FieldAndArguments.class));

    assertThat(actual.isEmpty()).isFalse();
    assertThat(actual.get(0).getMessage()).isEqualTo("username cannot be empty");
  }

  @Test
  public void testUsernameEmpty() {
    when(field.getName()).thenReturn("createNotification");
    when(fieldAndArguments.getField()).thenReturn(field);
    when(fieldAndArguments.getArgumentValue("username")).thenReturn("");
    when(environment.getFields()).thenReturn(Collections.singletonList(fieldAndArguments));
    when(environment.mkError(eq("username cannot be empty"), any(FieldAndArguments.class)))
        .thenReturn(buildError("username cannot be empty"));

    final List<GraphQLError> actual = validation.validateFields(environment);

    verify(environment).mkError(eq("username cannot be empty"), any(FieldAndArguments.class));

    assertThat(actual.isEmpty()).isFalse();
    assertThat(actual.get(0).getMessage()).isEqualTo("username cannot be empty");
  }

  @Test
  public void testUsernameTooShort() {
    when(field.getName()).thenReturn("createNotification");
    when(fieldAndArguments.getField()).thenReturn(field);
    when(fieldAndArguments.getArgumentValue("username")).thenReturn("aa");
    when(environment.getFields()).thenReturn(Collections.singletonList(fieldAndArguments));
    when(environment.mkError(
            eq("username must be between 3 and 64 characters"), any(FieldAndArguments.class)))
        .thenReturn(buildError("username must be between 3 and 64 characters"));

    final List<GraphQLError> actual = validation.validateFields(environment);

    verify(environment)
        .mkError(eq("username must be between 3 and 64 characters"), any(FieldAndArguments.class));

    assertThat(actual.isEmpty()).isFalse();
    assertThat(actual.get(0).getMessage())
        .isEqualTo("username must be between 3 and 64 characters");
  }

  @Test
  public void testUsernameTooLong() {
    when(field.getName()).thenReturn("createNotification");
    when(fieldAndArguments.getField()).thenReturn(field);

    final String username = String.join("", Collections.nCopies(7, "1234567890"));
    assertThat(username.length()).isGreaterThan(64);

    when(fieldAndArguments.getArgumentValue("username")).thenReturn(username);
    when(environment.getFields()).thenReturn(Collections.singletonList(fieldAndArguments));
    when(environment.mkError(
            eq("username must be between 3 and 64 characters"), any(FieldAndArguments.class)))
        .thenReturn(buildError("username must be between 3 and 64 characters"));

    final List<GraphQLError> actual = validation.validateFields(environment);

    verify(environment)
        .mkError(eq("username must be between 3 and 64 characters"), any(FieldAndArguments.class));

    assertThat(actual.isEmpty()).isFalse();
    assertThat(actual.get(0).getMessage())
        .isEqualTo("username must be between 3 and 64 characters");
  }

  @Test
  public void testUsernameInvalidCharacters() {
    when(field.getName()).thenReturn("createNotification");
    when(fieldAndArguments.getField()).thenReturn(field);
    when(fieldAndArguments.getArgumentValue("username")).thenReturn("test!!!");
    when(environment.getFields()).thenReturn(Collections.singletonList(fieldAndArguments));
    when(environment.mkError(
            eq("username must only contain alphanumeric characters"), any(FieldAndArguments.class)))
        .thenReturn(buildError("username must only contain alphanumeric characters"));

    final List<GraphQLError> actual = validation.validateFields(environment);

    verify(environment)
        .mkError(
            eq("username must only contain alphanumeric characters"), any(FieldAndArguments.class));

    assertThat(actual.isEmpty()).isFalse();
    assertThat(actual.get(0).getMessage())
        .isEqualTo("username must only contain alphanumeric characters");
  }

  public static GraphQLError buildError(final String message) {
    return new GraphQLError() {

      @Override
      public String getMessage() {
        return message;
      }

      @Nullable
      @Override
      public List<SourceLocation> getLocations() {
        return null;
      }

      @Nullable
      @Override
      public ErrorType getErrorType() {
        return null;
      }
    };
  }
}
