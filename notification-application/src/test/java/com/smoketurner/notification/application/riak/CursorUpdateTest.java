package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class CursorUpdateTest {

    @Test
    public void testUpdatesCursor() {
        final CursorUpdate update = new CursorUpdate("test-notifications",
                12345L);

        final CursorObject original = new CursorObject("test-notifications", 1L);

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
