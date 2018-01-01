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

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;

public class CursorObjectTest {

    private final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private CursorObject cursor;

    @Before
    public void setUp() {
        cursor = new CursorObject("test-notifications", 1234567L);
    }

    @Test
    public void serializesToJSON() throws Exception {
        final String actual = MAPPER.writeValueAsString(cursor);
        final String expected = MAPPER.writeValueAsString(MAPPER.readValue(
                fixture("fixtures/cursor.json"), CursorObject.class));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        final CursorObject actual = MAPPER
                .readValue(fixture("fixtures/cursor.json"), CursorObject.class);
        assertThat(actual).isEqualTo(cursor);
    }

    @Test
    public void testGetKey() {
        assertThat(cursor.getKey()).isEqualTo("test-notifications");
    }

    @Test
    public void testGetValue() {
        assertThat(cursor.getValue()).isEqualTo(1234567L);
    }

    @Test
    public void testToString() {
        assertThat(cursor.toString()).isEqualTo(
                "CursorObject{key=test-notifications, value=1234567}");
    }

    @Test
    public void testCursorSorting() {
        final CursorObject c2 = new CursorObject("test-notifications", 1L);
        final CursorObject c3 = new CursorObject("test-notifications", 2L);
        final TreeSet<CursorObject> cursors = new TreeSet<>();
        cursors.add(cursor);
        cursors.add(c2);
        cursors.add(c3);
        assertThat(cursors).containsExactly(cursor, c3, c2);
    }

    @Test
    public void testNaturalOrdering() {
        final CursorObject c1 = new CursorObject("test-notifications", 1L);
        final CursorObject c2 = new CursorObject("test-notifications", 2L);
        final CursorObject c3 = new CursorObject("test-other", 2L);
        assertThat(c1.equals(c2)).isEqualTo(c1.compareTo(c2) == 0);
        assertThat(c2.equals(c3)).isEqualTo(c2.compareTo(c3) == 0);
        assertThat(c1.equals(c3)).isEqualTo(c1.compareTo(c3) == 0);
    }
}
