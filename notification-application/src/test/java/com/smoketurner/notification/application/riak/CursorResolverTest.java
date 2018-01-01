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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

@SuppressWarnings("NullAway")
public class CursorResolverTest {

    private final CursorResolver resolver = new CursorResolver();

    @Test
    public void testNoSiblings() throws Exception {
        final List<CursorObject> siblings = Collections.emptyList();
        final CursorObject actual = resolver.resolve(siblings);
        assertThat(actual).isNull();
    }

    @Test
    public void testSingleSibling() throws Exception {
        final CursorObject list = new CursorObject("test", 1L);
        final List<CursorObject> siblings = Collections.singletonList(list);
        final CursorObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(list);
    }

    @Test
    public void testMultipleSibling() throws Exception {
        final CursorObject cursor1 = new CursorObject("test", 1L);
        final CursorObject cursor2 = new CursorObject("test", 2L);
        final CursorObject cursor3 = new CursorObject("test", 3L);
        final CursorObject cursor4 = new CursorObject("test", 4L);
        final CursorObject cursor5 = new CursorObject("test", 5L);
        final CursorObject cursor6 = new CursorObject("test", 6L);

        final List<CursorObject> siblings = Arrays.asList(cursor1, cursor2,
                cursor3, cursor4, cursor5, cursor6);

        final CursorObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(cursor6);
        assertThat(actual.getValue()).isEqualTo(6L);
    }
}
