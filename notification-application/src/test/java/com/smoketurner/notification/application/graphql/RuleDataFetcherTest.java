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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.store.RuleStore;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RuleDataFetcherTest {

  private final RuleStore store = mock(RuleStore.class);
  private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
  private final RuleDataFetcher fetcher = new RuleDataFetcher(store);

  @Test
  public void testEmptyRules() throws Exception {
    when(store.fetchCached()).thenReturn(Collections.emptyMap());

    final List<Map<String, Object>> actual = fetcher.get(environment);
    verify(store).fetchCached();

    assertThat(actual).isNotNull();
    assertThat(actual).isEmpty();
  }

  @Test
  public void testFetchRules() throws Exception {
    final Rule expected = Rule.builder().withMaxSize(3).build();
    final Map<String, Rule> rules = new HashMap<>();
    rules.put("like", expected);

    when(store.fetchCached()).thenReturn(rules);

    final List<Map<String, Object>> actual = fetcher.get(environment);
    verify(store).fetchCached();

    assertThat(actual).isNotNull();
    assertThat(actual.get(0).get("category")).isEqualTo("like");
    assertThat(actual.get(0).get("rule")).isEqualTo(expected);
  }
}
