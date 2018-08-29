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
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;

public class StringSetParamTest {

  @Test
  public void testParse() throws Exception {
    final StringSetParam param = new StringSetParam("1,3, 2 ,asdf,   3");
    final Set<String> expected = ImmutableSet.of("1", "2", "3");
    assertThat(param.parse("1,2,asdf,3")).containsAll(expected);
  }
}