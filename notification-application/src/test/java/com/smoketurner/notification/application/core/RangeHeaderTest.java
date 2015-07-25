package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class RangeHeaderTest {

    @Test
    public void testParse() throws Exception {
        RangeHeader expected = new RangeHeader("id", 624452251888521216L, 1);
        RangeHeader actual = RangeHeader.parse("id 624452251888521216; max=1");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader("id", 624452251888521216L, null);
        actual = RangeHeader.parse("id 624452251888521216");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader("id", 624452251888521216L, null);
        actual = RangeHeader.parse("id 624452251888521216;");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader("id", 624452251888521216L, 1);
        actual = RangeHeader.parse("id 624452251888521216   ;    max=1");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader("id", 624452251888521216L, 1);
        actual = RangeHeader.parse("id 624452251888521216..; max=1");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader("id", 624452251888521216L, 1);
        actual = RangeHeader.parse("id ]624452251888521216..; max=1");
        assertThat(actual).isEqualTo(expected);

        expected = new RangeHeader(null, null, 1);
        actual = RangeHeader.parse("id; max=1");
        assertThat(actual).isEqualTo(expected);
    }
}
