/**
 * Copyright 2017 Smoke Turner, LLC.
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
package com.smoketurner.notification.api;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.util.Duration;

public class RuleTest {
    private final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private final Rule rule = Rule.builder().withMatchOn("first_name")
            .withMaxSize(3).withMaxDuration(Duration.minutes(10)).build();;

    @Test
    public void serializesToJSON() throws Exception {
        final String actual = MAPPER.writeValueAsString(rule);
        final String expected = MAPPER.writeValueAsString(
                MAPPER.readValue(fixture("fixtures/rule.json"), Rule.class));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        final Rule actual = MAPPER.readValue(fixture("fixtures/rule.json"),
                Rule.class);
        assertThat(actual).isEqualTo(rule);
    }

    @Test
    public void testInvalidDuration() throws Exception {
        try {
            MAPPER.readValue(fixture("fixtures/rule_invalid.json"), Rule.class);
            failBecauseExceptionWasNotThrown(JsonMappingException.class);
        } catch (JsonMappingException e) {
        }
    }

    @Test
    public void testInvalidTimeUnit() throws Exception {
        try {
            MAPPER.readValue(fixture("fixtures/rule_invalid_timeunit.json"),
                    Rule.class);
            failBecauseExceptionWasNotThrown(JsonMappingException.class);
        } catch (JsonMappingException e) {
        }
    }

    @Test
    public void testIsValidMatchOn() throws Exception {
        Rule rule = Rule.builder().build();
        assertThat(rule.isValid()).isFalse();

        rule = Rule.builder().withMatchOn(null).build();
        assertThat(rule.isValid()).isFalse();

        rule = Rule.builder().withMatchOn("").build();
        assertThat(rule.isValid()).isTrue();
    }
}
