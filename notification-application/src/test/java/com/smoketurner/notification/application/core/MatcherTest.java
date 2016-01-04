/**
 * Copyright 2016 Smoke Turner, LLC.
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
import org.joda.time.DateTime;
import org.junit.Test;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Notification;
import io.dropwizard.util.Duration;

public class MatcherTest {

    private final Notification notification = Notification.builder().withId(1L)
            .withCategory("new-follower")
            .withProperties(
                    ImmutableMap.of("first_name", "Bob", "last_name", "Smith"))
            .withCreatedAt(new DateTime("2015-07-31T23:21:35Z")).build();

    @Test
    public void testCheckMatch() {
        final Rule rule = new Rule(Optional.<Integer> absent(),
                Optional.<Duration> absent(), Optional.of("first_name"));

        final Matcher matcher = new Matcher(rule, notification);

        final Notification n1 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Ted")).build();
        final Notification n3 = Notification.builder()
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder().build();

        assertThat(matcher.checkMatch(n1)).isTrue();
        assertThat(matcher.checkMatch(n2)).isFalse();
        assertThat(matcher.checkMatch(n3)).isFalse();
        assertThat(matcher.checkMatch(n4)).isFalse();
    }

    @Test
    public void testCheckMatchInitialNotSet() {
        final Notification notification = Notification.builder().build();
        final Rule rule = new Rule(Optional.<Integer> absent(),
                Optional.<Duration> absent(), Optional.of("first_name"));

        final Matcher matcher = new Matcher(rule, notification);

        final Notification n1 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Ted")).build();
        final Notification n3 = Notification.builder()
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder().build();

        assertThat(matcher.checkMatch(n1)).isFalse();
        assertThat(matcher.checkMatch(n2)).isFalse();
        assertThat(matcher.checkMatch(n3)).isFalse();
        assertThat(matcher.checkMatch(n4)).isFalse();
    }

    @Test
    public void testCheckSize() {
        final Rule rule = new Rule(Optional.of(2), Optional.<Duration> absent(),
                Optional.<String> absent());

        final Notification n2 = Notification.builder().withId(2L).build();
        final Notification n3 = Notification.builder().withId(3L).build();

        final Matcher matcher = new Matcher(rule, notification);
        assertThat(matcher.checkSize()).isTrue();
        matcher.add(ImmutableList.of(n2, n3));
        assertThat(matcher.checkSize()).isFalse();
    }

    @Test
    public void testCheckDuration() {
        final Rule rule = new Rule(Optional.<Integer> absent(),
                Optional.of(Duration.minutes(10)), Optional.<String> absent());

        final Notification future = Notification.builder()
                .withCreatedAt(new DateTime("2015-07-31T23:31:35Z")).build();
        final Notification future2 = Notification.builder()
                .withCreatedAt(new DateTime("2015-07-31T23:21:36Z")).build();
        final Notification present = Notification.builder()
                .withCreatedAt(new DateTime("2015-07-31T23:21:35Z")).build();
        final Notification past = Notification.builder()
                .withCreatedAt(new DateTime("2015-07-31T23:11:35Z")).build();
        final Notification past2 = Notification.builder()
                .withCreatedAt(new DateTime("2015-07-31T23:11:34Z")).build();

        final Matcher matcher = new Matcher(rule, notification);
        assertThat(matcher.checkDuration(future)).isFalse();
        assertThat(matcher.checkDuration(future2)).isFalse();
        assertThat(matcher.checkDuration(present)).isTrue();
        assertThat(matcher.checkDuration(past)).isTrue();
        assertThat(matcher.checkDuration(past2)).isFalse();
    }
}
