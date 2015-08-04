/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class RangeHeaderTest {

  @Test
  public void testParse() throws Exception {
    RangeHeader expected = new RangeHeader("id", 1L, true, 26L, true, 1);
    RangeHeader actual = RangeHeader.parse("id 1..26; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", 1L, true, 26L, true, null);
    actual = RangeHeader.parse("id 1..26");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", 1L, true, 26L, true, null);
    actual = RangeHeader.parse("id 1..26;");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", 1L, true, 26L, true, 1);
    actual = RangeHeader.parse("id 1..26   ;    max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", 1L, true, null, null, 1);
    actual = RangeHeader.parse("id 1..; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", 1L, false, null, null, 1);
    actual = RangeHeader.parse("id ]1..; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", null, null, 26L, true, 1);
    actual = RangeHeader.parse("id ..26; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader("id", null, null, 26L, false, 1);
    actual = RangeHeader.parse("id ..26[; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader(null, null, null, null, null, 1);
    actual = RangeHeader.parse("id; max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader(null, null, null, null, null, 1);
    actual = RangeHeader.parse("id;max=1");
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader(null, null, null, null, null, null);
    actual = RangeHeader.parse(null);
    assertThat(actual).isEqualTo(expected);

    expected = new RangeHeader(null, null, null, null, null, null);
    actual = RangeHeader.parse("");
    assertThat(actual).isEqualTo(expected);
  }
}
