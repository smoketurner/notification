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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smoketurner.dropwizard.graphql.GraphQLValidationError;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Test;

public class RemoveRuleMutationTest {

  private final RuleStore store = mock(RuleStore.class);
  private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
  private final RemoveRuleMutation mutation = new RemoveRuleMutation(store);

  @Test
  public void testCategoryNull() throws Exception {
    when(environment.getArgument("category")).thenReturn(null);

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("category cannot be empty");
    }

    verify(store, never()).remove(anyString());
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

    verify(store, never()).remove(anyString());
  }

  @Test
  public void testStoreException() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");
    doThrow(new NotificationStoreException()).when(store).remove(anyString());

    try {
      mutation.get(environment);
      failBecauseExceptionWasNotThrown(GraphQLValidationError.class);
    } catch (GraphQLValidationError e) {
      assertThat(e.getMessage()).isEqualTo("Unable to remove rule");
    }

    verify(store).remove(eq("like"));
  }

  @Test
  public void testRemoveRule() throws Exception {
    when(environment.getArgument("category")).thenReturn("like");

    final Boolean actual = mutation.get(environment);
    verify(store).remove(eq("like"));

    assertThat(actual).isTrue();
  }
}
