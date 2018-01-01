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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;

public class Matcher implements Predicate<Notification>, Comparable<Matcher> {

    private final TreeSet<Notification> notifications = new TreeSet<>();
    private final Notification notification;
    private final Rule rule;
    private final int maxSize;
    private final long firstMillis;
    private final long maxDuration;

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
        this.rule = Objects.requireNonNull(rule, "rule == null");
        this.notification = Objects.requireNonNull(notification,
                "notification == null");

        this.maxSize = rule.getMaxSize().orElse(0);
        this.matchOn = rule.getMatchOn().orElse(null);
        if (matchOn != null) {
            this.matchValue = notification.getProperties().get(matchOn);
        } else {
            this.matchValue = null;
        }
        if (rule.getMaxDuration().isPresent()) {
            this.maxDuration = rule.getMaxDuration().get().toMilliseconds();
            this.firstMillis = notification.getCreatedAt().toInstant()
                    .toEpochMilli();
        } else {
            this.maxDuration = 0;
            this.firstMillis = 0;
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
     * @return true if the matcher is full, otherwise false
     */
    public boolean isFull() {
        return (maxSize > 0 && notifications.size() >= maxSize);
    }

    /**
     * Check whether the given notification matches the same category as the
     * original notification.
     *
     * @param notification
     *            Notification to check
     * @return true if the notification category matches, otherwise false
     */
    public boolean checkCategory(@Nonnull final Notification notification) {
        return Objects.equals(this.notification.getCategory(),
                notification.getCategory());
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

        if (maxDuration > 0 && firstMillis > 0) {
            final long delta = firstMillis
                    - notification.getCreatedAt().toInstant().toEpochMilli();
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
        if (notification != null && notification.getId().isPresent()
                && checkCategory(notification) && !isFull()
                && checkDuration(notification) && checkMatch(notification)) {
            return notifications.add(notification);
        }
        return false;
    }

    @VisibleForTesting
    boolean add(@Nonnull final Collection<Notification> notifications) {
        return this.notifications.addAll(notifications);
    }

    public Notification getNotification() {
        if (notifications.isEmpty()) {
            return notification;
        }
        return Notification.builder(notification)
                .withNotifications(notifications).build();
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
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
        return notification.hashCode();
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
        return notification.compareTo(that.notification);
    }
}
