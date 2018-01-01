/**
 * Copyright 2018 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class CursorUpdateTest {

    @Test
    public void testUpdatesCursor() {
        final CursorUpdate update = new CursorUpdate("test-notifications",
                12345L);

        final CursorObject original = new CursorObject("test-notifications",
                1L);

        final CursorObject expected = new CursorObject("test-notifications",
                12345L);

        final CursorObject actual = update.apply(original);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNoOriginal() {
        final CursorUpdate update = new CursorUpdate("test-notifications",
                12345L);

        final CursorObject expected = new CursorObject("test-notifications",
                12345L);

        final CursorObject actual = update.apply(null);
        assertThat(actual).isEqualTo(expected);
    }
}
