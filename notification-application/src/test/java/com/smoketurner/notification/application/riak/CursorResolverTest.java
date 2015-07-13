package com.smoketurner.notification.application.riak;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

public class CursorResolverTest {

    private final CursorResolver resolver = new CursorResolver();

    @Test
    public void testNoSiblings() throws Exception {
        final List<CursorObject> siblings = ImmutableList.of();
        final CursorObject actual = resolver.resolve(siblings);
        assertThat(actual).isNull();
    }

    @Test
    public void testSingleSibling() throws Exception {
        final CursorObject list = new CursorObject("test", 1L);
        final List<CursorObject> siblings = ImmutableList.of(list);
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

        final List<CursorObject> siblings = ImmutableList.of(cursor1, cursor2,
                cursor3, cursor4, cursor5, cursor6);

        final CursorObject actual = resolver.resolve(siblings);
        assertThat(actual).isEqualTo(cursor6);
        assertThat(actual.getValue()).isEqualTo(6L);
    }
}
