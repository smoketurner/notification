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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.smoketurner.notification.api.Notification;

public class Matcher implements Predicate<Notification>, Comparable<Matcher> {

    @NotNull
    private final Notification notification;

    @NotNull
    private final Rule rule;

    private final TreeSet<Notification> notifications = new TreeSet<>();

    @Nullable
    private final Integer maxSize;

    @Nullable
    private final Long firstMillis;

    @Nullable
    private final Long maxDuration;

    @Nullable
    private final String matchOn;

    @Nullable
    private final String matchValue;

    /**
     * Constructor
     *
     * @param rule
     *            Rule for this match
     * @param notification
     *            First notification to match the rule
     */
    public Matcher(@Nonnull final Rule rule,
            @Nonnull final Notification notification) {
        this.rule = Objects.requireNonNull(rule);
        this.notification = Objects.requireNonNull(notification);

        this.matchOn = rule.getMatchOn().orNull();
        this.maxSize = rule.getMaxSize().orNull();
        if (matchOn != null) {
            this.matchValue = notification.getProperties().get(matchOn);
        } else {
            this.matchValue = null;
        }
        if (rule.getMaxDuration().isPresent()) {
            this.maxDuration = rule.getMaxDuration().get().toMilliseconds();
            this.firstMillis = notification.getCreatedAt().getMillis();
        } else {
            this.maxDuration = null;
            this.firstMillis = null;
        }
    }

    /**
     * Checks whether the notification contains the "match-on" field and the
     * value matches the same value as the first notification in the matcher.
     * 
     * @param notification
     *            Notification to match
     * @return true if the notification matches, otherwise false
     */
    public boolean checkMatch(@Nonnull final Notification notification) {
        if (!rule.getMatchOn().isPresent()) {
            return true;
        }

        if (matchOn == null || matchValue == null) {
            return false;
        }

        final Map<String, String> properties = notification.getProperties();
        return properties.containsKey(matchOn)
                && Objects.equals(properties.get(matchOn), matchValue);
    }

    /**
     * Check whether this matcher has reached "max-size" or not.
     *
     * @return true if we can put more notifications in this matcher, otherwise
     *         false.
     */
    public boolean checkSize() {
        if (maxSize != null && notifications.size() >= maxSize) {
            return false;
        }
        return true;
    }

    /**
     * Check whether the given notification is within the "max-duration" for
     * this matcher or not.
     *
     * @param notification
     *            Notification to check
     * @return true if the notification is within the maximum duration,
     *         otherwise false.
     */
    public boolean checkDuration(@Nonnull final Notification notification) {
        if (!rule.getMaxDuration().isPresent()) {
            return true;
        }

        if (maxDuration != null && firstMillis != null) {
            final long timestamp = notification.getCreatedAt().getMillis();
            final long delta = firstMillis - timestamp;
            if (delta >= 0 && delta <= maxDuration) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add the given notification into this matcher if the checks pass.
     *
     * @param notification
     *            Notification to add
     * @return true if the notification can be added, otherwise false.
     */
    @Override
    public boolean test(@Nonnull final Notification notification) {
        if (checkSize() && checkDuration(notification)
                && checkMatch(notification)
                && !this.notification.equals(notification)) {
            return notifications.add(notification);
        }
        return false;
    }

    @VisibleForTesting
    protected boolean add(
            @Nonnull final Collection<Notification> notifications) {
        return this.notifications.addAll(notifications);
    }

    public long getId() {
        return notification.getId(0L);
    }

    public Notification getNotification() {
        if (notifications.isEmpty()) {
            return notification;
        }
        return Notification.builder(notification)
                .withNotifications(notifications).build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final Matcher other = (Matcher) obj;
        return Objects.equals(notification, other.notification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notification);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("notification", notification).add("rule", rule)
                .add("notifications", notifications).add("maxSize", maxSize)
                .add("firstMillis", firstMillis).add("maxDuration", maxDuration)
                .add("matchOn", matchOn).add("matchValue", matchValue)
                .toString();
    }

    @Override
    public int compareTo(final Matcher that) {
        return ComparisonChain.start().compare(this.getId(), that.getId(),
                Ordering.natural().reverse()).result();
    }
}
