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
