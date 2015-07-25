package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.Test;
import com.google.common.collect.ImmutableSet;

public class LongSetParamTest {

    @Test
    public void testParse() throws Exception {
        final LongSetParam param = new LongSetParam("1,3, 2 ,asdf,   3");
        final Set<Long> expected = ImmutableSet.of(1L, 2L, 3L);
        assertThat(param.parse("1,2,asdf,3")).containsAll(expected);
    }
}
