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

import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.smoketurner.notification.api.Notification;

@Immutable
public final class UserNotifications {

    private final Iterable<Notification> unseen;
    private final Iterable<Notification> seen;

    /**
     * Constructor
     *
     * @param unseen
     *            Unseen notifications
     * @param seen
     *            Seen notifications
     */
    public UserNotifications(@Nonnull final Iterable<Notification> unseen,
            @Nonnull final Iterable<Notification> seen) {
        this.unseen = Objects.requireNonNull(unseen, "unseen == null");
        this.seen = Objects.requireNonNull(seen, "seen == null");
    }

    /**
     * Constructor
     *
     * @param unseen
     *            Unseen notifications
     * @param seen
     *            Seen notifications
     */
    public UserNotifications(@Nonnull final Stream<Notification> unseen,
            @Nonnull final Stream<Notification> seen) {
        Objects.requireNonNull(unseen, "unseen == null");
        Objects.requireNonNull(seen, "seen == null");

        this.unseen = unseen.collect(Collectors.toCollection(TreeSet::new));
        this.seen = seen.collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Constructor
     *
     * @param unseen
     *            Unseen notifications
     */
    public UserNotifications(@Nonnull final Iterable<Notification> unseen) {
        this.unseen = Objects.requireNonNull(unseen, "unseen == null");
        this.seen = Collections.<Notification>emptySortedSet();
    }

    /**
     * Constructor
     *
     * @param unseen
     *            Unseen notifications
     */
    public UserNotifications(@Nonnull final Stream<Notification> unseen) {
        Objects.requireNonNull(unseen, "unseen == null");
        this.unseen = unseen.collect(Collectors.toCollection(TreeSet::new));
        this.seen = Collections.<Notification>emptySortedSet();
    }

    /**
     * Constructor
     */
    public UserNotifications() {
        this.unseen = Collections.<Notification>emptySortedSet();
        this.seen = Collections.<Notification>emptySortedSet();
    }

    public boolean isEmpty() {
        return Iterables.isEmpty(unseen) && Iterables.isEmpty(seen);
    }

    public Iterable<Notification> getUnseen() {
        return unseen;
    }

    public Iterable<Notification> getSeen() {
        return seen;
    }

    public ImmutableSortedSet<Notification> getNotifications() {
        return ImmutableSortedSet.<Notification>naturalOrder().addAll(unseen)
                .addAll(seen).build();
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final UserNotifications other = (UserNotifications) obj;
        return Objects.equals(unseen, other.unseen)
                && Objects.equals(seen, other.seen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unseen, seen);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("unseen", unseen)
                .add("seen", seen).toString();
    }
}
