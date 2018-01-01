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
package com.smoketurner.notification.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.Test;

public class RangeHeaderTest {

    @Test
    public void testParse() throws Exception {
        RangeHeader expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(true).toId(26L).toInclusive(true).max(1).build();
        RangeHeader actual = RangeHeader.parse("id 1..26; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(true);
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(true);
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(true).toId(26L).toInclusive(true).build();
        actual = RangeHeader.parse("id 1..26");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(true);
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(true);
        assertThat(actual.getMax()).isEqualTo(Optional.empty());

        expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(true).toId(26L).toInclusive(true).build();
        actual = RangeHeader.parse("id 1..26;");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(true);
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(true);
        assertThat(actual.getMax()).isEqualTo(Optional.empty());

        expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(true).toId(26L).toInclusive(true).max(1).build();
        actual = RangeHeader.parse("id 1..26   ;    max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(true);
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(true);
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(true).max(1).build();
        actual = RangeHeader.parse("id 1..; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(true);
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().field("id").fromId(1L)
                .fromInclusive(false).max(1).build();
        actual = RangeHeader.parse("id ]1..; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId().get()).isEqualTo(1L);
        assertThat(actual.getFromInclusive().get()).isEqualTo(false);
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().field("id").toId(26L).toInclusive(true)
                .max(1).build();
        actual = RangeHeader.parse("id ..26; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(true);
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().field("id").toId(26L)
                .toInclusive(false).max(1).build();
        actual = RangeHeader.parse("id ..26[; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField().get()).isEqualTo("id");
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId().get()).isEqualTo(26L);
        assertThat(actual.getToInclusive().get()).isEqualTo(false);
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().max(1).build();
        actual = RangeHeader.parse("id; max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField()).isEqualTo(Optional.empty());
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.builder().max(1).build();
        actual = RangeHeader.parse("id;max=1");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField()).isEqualTo(Optional.empty());
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax().get()).isEqualTo(1);

        expected = RangeHeader.create();
        actual = RangeHeader.parse(null);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField()).isEqualTo(Optional.empty());
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax()).isEqualTo(Optional.empty());

        expected = RangeHeader.create();
        actual = RangeHeader.parse("");
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getField()).isEqualTo(Optional.empty());
        assertThat(actual.getFromId()).isEqualTo(Optional.empty());
        assertThat(actual.getFromInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getToId()).isEqualTo(Optional.empty());
        assertThat(actual.getToInclusive()).isEqualTo(Optional.empty());
        assertThat(actual.getMax()).isEqualTo(Optional.empty());
    }
}
