package com.smoketurner.notification.application.core;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
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
        this.unseen = Preconditions.checkNotNull(unseen);
        this.seen = Preconditions.checkNotNull(seen);
    }

    /**
     * Constructor
     *
     * @param unseen
     *            Unseen notifications
     */
    public UserNotifications(@Nonnull final Iterable<Notification> unseen) {
        this.unseen = Preconditions.checkNotNull(unseen);
        this.seen = ImmutableSortedSet.of();
    }

    /**
     * Constructor
     */
    public UserNotifications() {
        this.unseen = ImmutableSortedSet.of();
        this.seen = ImmutableSortedSet.of();
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
        return ImmutableSortedSet.<Notification> naturalOrder().addAll(unseen)
                .addAll(seen).build();
    }

    @Override
    public boolean equals(final Object obj) {
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
