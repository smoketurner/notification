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

import com.smoketurner.dropwizard.graphql.GraphQLValidationError;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveAllRulesMutation implements DataFetcher<Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAllRulesMutation.class);
  private final RuleStore store;

  /**
   * Constructor
   *
   * @param store Rule data store
   */
  public RemoveAllRulesMutation(final RuleStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Override
  public Boolean get(DataFetchingEnvironment environment) {
    try {
      store.removeAll();
    } catch (NotificationStoreException e) {
      LOGGER.error("Unable to remove all rules", e);
      throw new GraphQLValidationError("Unable to remove all rules");
    }

    return true;
  }
}
