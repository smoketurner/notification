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
package com.smoketurner.notification.application.riak;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.basho.riak.client.api.annotations.RiakBucketName;
import com.basho.riak.client.api.annotations.RiakContentType;
import com.basho.riak.client.api.annotations.RiakKey;
import com.basho.riak.client.api.annotations.RiakLastModified;
import com.basho.riak.client.api.annotations.RiakTombstone;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.annotations.RiakVTag;
import com.basho.riak.client.api.cap.VClock;
import com.google.common.base.MoreObjects;
import com.smoketurner.notification.api.Notification;

public class NotificationListObject {

    private static final int MAX_NOTIFICATIONS = 1000;

    @RiakBucketName
    private final String bucketName = "notifications";

    @RiakKey
    @Nullable
    private String key;

    @RiakVClock
    @Nullable
    private VClock vclock;

    @RiakTombstone
    @Nullable
    private Boolean tombstone;

    @RiakContentType
    @Nullable
    private String contentType;

    @RiakLastModified
    @Nullable
    private Long lastModified;

    @RiakVTag
    @Nullable
    private String vtag;

    private final TreeSet<Notification> notifications = new TreeSet<>();
    private final Set<Long> deletedIds = new HashSet<>();

    /**
     * Constructor
     */
    public NotificationListObject() {
        // needed to handle tombstones
    }

    /**
     * Constructor
     *
     * @param key
     */
    public NotificationListObject(@Nonnull final String key) {
        this.key = Objects.requireNonNull(key, "key == null");
    }

    public void addNotification(final Notification notification) {
        notifications.add(notification);
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.pollLast();
        }
    }

    public void addNotifications(final Collection<Notification> notifications) {
        this.notifications.addAll(notifications);
        while (this.notifications.size() > MAX_NOTIFICATIONS) {
            this.notifications.pollLast();
        }
    }

    public void deleteNotification(final long id) {
        deletedIds.add(id);
    }

    public void deleteNotifications(final Collection<Long> ids) {
        deletedIds.addAll(ids);
    }

    @Nullable
    public String getKey() {
        return key;
    }

    public SortedSet<Notification> getNotifications() {
        return notifications;
    }

    public Set<Long> getDeletedIds() {
        return deletedIds;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final NotificationListObject other = (NotificationListObject) obj;
        return Objects.equals(key, other.key)
                && Objects.equals(vclock, other.vclock)
                && Objects.equals(tombstone, other.tombstone)
                && Objects.equals(contentType, other.contentType)
                && Objects.equals(lastModified, other.lastModified)
                && Objects.equals(vtag, other.vtag)
                && Objects.equals(notifications, other.notifications)
                && Objects.equals(deletedIds, other.deletedIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, vclock, tombstone, contentType, lastModified,
                vtag, notifications, deletedIds);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("key", key)
                .add("vclock", vclock).add("tombstone", tombstone)
                .add("contentType", contentType)
                .add("lastModified", lastModified).add("vtag", vtag)
                .add("notifications", notifications)
                .add("deletedIds", deletedIds).toString();
    }
}
