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

import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RuleDataFetcher implements DataFetcher<List<Map<String, Object>>> {

  private final RuleStore store;

  /**
   * Constructor
   *
   * @param store Rule data store
   */
  public RuleDataFetcher(final RuleStore store) {
    this.store = Objects.requireNonNull(store, "store == null");
  }

  @Override
  public List<Map<String, Object>> get(DataFetchingEnvironment environment) {
    final Map<String, Rule> data = store.fetchCached();

    return data.entrySet().stream()
        .map(e -> ImmutableMap.of("category", e.getKey(), "rule", e.getValue()))
        .collect(Collectors.toList());
  }
}
