package com.smoketurner.notification.application.riak;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.jackson.Jackson;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

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
        final CursorObject actual = MAPPER.readValue(
                fixture("fixtures/cursor.json"), CursorObject.class);
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
        final TreeSet<CursorObject> cursors = Sets.newTreeSet();
        cursors.add(cursor);
        cursors.add(c2);
        cursors.add(c3);
        assertThat(cursors).containsExactly(cursor, c3, c2);
    }
}
