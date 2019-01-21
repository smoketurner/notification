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
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetchingEnvironment;
import io.dropwizard.util.Duration;
import java.util.Collections;
import org.junit.Test;

public class CreateRuleMutationTest {

  private final RuleStore store = mock(RuleStore.class);
  private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
  private final CreateRuleMutation mutation = new CreateRuleMutation(store);

  @Test
  public void testCategoryNull() throws Exception {
    when(environment.getArgument("category")).thenReturn(null);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testCategoryEmpty() throws Exception {
    when(environment.getArgument("category")).thenReturn("");

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testCategoryTooShort() throws Exception {
    when(environment.getArgument("category")).thenReturn("aa");

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category must be between 3 and 20 characters");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testCategoryTooLong() throws Exception {
    when(environment.getArgument("category")).thenReturn("thisisareallylongcategory");

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category must be between 3 and 20 characters");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testRuleNull() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule")).thenReturn(null);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("rule cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testRuleEmpty() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule")).thenReturn(Collections.emptyMap());

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("rule cannot be empty");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testRuleInvalidSize() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule")).thenReturn(ImmutableMap.of("maxSize", ""));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("maxSize is not an integer");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testRuleInvalidDuration() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule")).thenReturn(ImmutableMap.of("maxDuration", ""));

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("maxDuration is an invalid duration");
    }

    verify(store, never()).store(anyString(), any(Rule.class));
  }

  @Test
  public void testStoreException() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule")).thenReturn(ImmutableMap.of("maxSize", 3));
    doThrow(new NotificationStoreException()).when(store).store(anyString(), any(Rule.class));

    final Rule rule = Rule.builder().withMaxSize(3).build();

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("Unable to create rule");
    }

    verify(store).store(eq("like"), eq(rule));
  }

  @Test
  public void testStoreRule() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    when(environment.getArgument("rule"))
        .thenReturn(ImmutableMap.of("maxSize", 3, "maxDuration", "3m", "matchOn", "like_id"));

    final Rule rule =
        Rule.builder()
            .withMaxSize(3)
            .withMatchOn("like_id")
            .withMaxDuration(Duration.minutes(3))
            .build();

    final Boolean actual = mutation.get(environment);
    verify(store).store(eq("like"), eq(rule));
    assertThat(actual).isTrue();
  }
}
