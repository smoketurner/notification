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
import java.time.ZonedDateTime;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;
import io.dropwizard.util.Duration;

public class MatcherTest {

    private final Notification notification = Notification
            .builder("new-follower").withId(1L)
            .withProperties(
                    ImmutableMap.of("first_name", "Bob", "last_name", "Smith"))
            .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:21:35Z")).build();

    @Test
    public void testMatch() {
        final Rule rule = Rule.builder().withMatchOn("first_name").build();

        final Matcher matcher = new Matcher(rule, notification);

        final Notification n1 = Notification.builder("new-follower").withId(2L)
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder("new-follower").withId(3L)
                .withProperties(ImmutableMap.of("first_name", "Ted")).build();
        final Notification n3 = Notification.builder("new-follower").withId(4L)
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder().build();
        final Notification n5 = Notification.builder("like").withId(5L).build();

        assertThat(matcher.test(n1)).isTrue();
        assertThat(matcher.test(n2)).isFalse();
        assertThat(matcher.test(n3)).isFalse();
        assertThat(matcher.test(n4)).isFalse();
        assertThat(matcher.test(n5)).isFalse();
    }

    @Test
    public void testCheckMatch() {
        final Rule rule = Rule.builder().withMatchOn("first_name").build();

        final Matcher matcher = new Matcher(rule, notification);

        final Notification n1 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Ted")).build();
        final Notification n3 = Notification.builder()
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder().build();
        final Notification n5 = Notification.builder("like").build();

        assertThat(matcher.checkMatch(n1)).isTrue();
        assertThat(matcher.checkMatch(n2)).isFalse();
        assertThat(matcher.checkMatch(n3)).isFalse();
        assertThat(matcher.checkMatch(n4)).isFalse();
        assertThat(matcher.checkMatch(n5)).isFalse();
    }

    @Test
    public void testCheckMatchInitialNotSet() {
        final Notification notification = Notification.builder().build();
        final Rule rule = Rule.builder().withMatchOn("first_name").build();

        final Matcher matcher = new Matcher(rule, notification);

        final Notification n1 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Bob")).build();
        final Notification n2 = Notification.builder()
                .withProperties(ImmutableMap.of("first_name", "Ted")).build();
        final Notification n3 = Notification.builder()
                .withProperties(ImmutableMap.of("last_name", "Smith")).build();
        final Notification n4 = Notification.builder().build();
        final Notification n5 = Notification.builder("like").build();

        assertThat(matcher.checkMatch(n1)).isFalse();
        assertThat(matcher.checkMatch(n2)).isFalse();
        assertThat(matcher.checkMatch(n3)).isFalse();
        assertThat(matcher.checkMatch(n4)).isFalse();
        assertThat(matcher.checkMatch(n5)).isFalse();
    }

    @Test
    public void testCheckSize() {
        final Rule rule = Rule.builder().withMaxSize(2).build();

        final Notification n2 = Notification.builder().withId(2L).build();
        final Notification n3 = Notification.builder().withId(3L).build();

        final Matcher matcher = new Matcher(rule, notification);
        assertThat(matcher.isFull()).isFalse();
        matcher.add(ImmutableList.of(n2, n3));
        assertThat(matcher.isFull()).isTrue();
    }

    @Test
    public void testCheckDuration() {
        final Rule rule = Rule.builder().withMaxDuration(Duration.minutes(10))
                .build();

        final Notification future = Notification.builder()
                .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:31:35Z"))
                .build();
        final Notification future2 = Notification.builder()
                .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:21:36Z"))
                .build();
        final Notification present = Notification.builder()
                .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:21:35Z"))
                .build();
        final Notification past = Notification.builder()
                .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:11:35Z"))
                .build();
        final Notification past2 = Notification.builder()
                .withCreatedAt(ZonedDateTime.parse("2015-07-31T23:11:34Z"))
                .build();

        final Matcher matcher = new Matcher(rule, notification);
        assertThat(matcher.checkDuration(future)).isFalse();
        assertThat(matcher.checkDuration(future2)).isFalse();
        assertThat(matcher.checkDuration(present)).isTrue();
        assertThat(matcher.checkDuration(past)).isTrue();
        assertThat(matcher.checkDuration(past2)).isFalse();
    }

    @Test
    public void testNaturalOrdering() {
        final Rule rule1 = Rule.builder().withMatchOn("first_name").build();
        final Rule rule2 = Rule.builder().withMatchOn("last_name").build();

        final Notification n1 = Notification.builder().withId(1L).build();
        final Notification n2 = Notification.builder().withId(2L).build();

        final Matcher m1 = new Matcher(rule1, n1);
        final Matcher m2 = new Matcher(rule1, n2);
        final Matcher m3 = new Matcher(rule2, n1);
        assertThat(m1.equals(m2)).isEqualTo(m1.compareTo(m2) == 0);
        assertThat(m2.equals(m3)).isEqualTo(m2.compareTo(m3) == 0);
        assertThat(m1.equals(m3)).isEqualTo(m1.compareTo(m3) == 0);
    }
}
