package com.smoketurner.notification.application.riak;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.basho.riak.client.api.annotations.RiakBucketName;
import com.basho.riak.client.api.annotations.RiakContentType;
import com.basho.riak.client.api.annotations.RiakKey;
import com.basho.riak.client.api.annotations.RiakLastModified;
import com.basho.riak.client.api.annotations.RiakTombstone;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.annotations.RiakVTag;
import com.basho.riak.client.api.cap.VClock;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;

public final class NotificationListObject {

    private static final int MAX_NOTIFICATIONS = 1000;

    @RiakBucketName
    private final String bucket = "notifications";

    @RiakKey
    private String key;

    @RiakVClock
    private VClock vclock;

    @RiakTombstone
    private boolean tombstone;

    @RiakContentType
    private String contentType;

    @RiakLastModified
    private Long lastModified;

    @RiakVTag
    private String vtag;

    private final TreeSet<Notification> notifications = Sets.newTreeSet();

    private final Set<Long> deletedIds = Sets.newHashSet();

    /**
     * Constructor
     */
    public NotificationListObject() {
    }

    /**
     * Constructor
     *
     * @param key
     */
    public NotificationListObject(final String key) {
        this.key = key;
    }

    /**
     * Constructor
     * 
     * @param other
     */
    public NotificationListObject(final NotificationListObject other) {
        this.key = other.key;
        this.vclock = other.vclock;
        this.tombstone = other.tombstone;
        this.contentType = other.contentType;
        this.lastModified = other.lastModified;
        this.vtag = other.vtag;
        this.addNotifications(other.getNotificationList());
        this.deleteNotifications(other.getDeletedIds());
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

    public void deleteNotifications(final Collection<Long> deletedIds) {
        this.deletedIds.addAll(deletedIds);
    }

    public void clearDeletedIds() {
        deletedIds.clear();
    }

    public String getKey() {
        return key;
    }

    public List<Notification> getNotificationList() {
        return ImmutableList.copyOf(notifications);
    }

    public Set<Long> getDeletedIds() {
        return deletedIds;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final NotificationListObject other = (NotificationListObject) obj;
        return Objects.equal(bucket, other.bucket)
                && Objects.equal(key, other.key)
                && Objects.equal(vclock, other.vclock)
                && Objects.equal(tombstone, other.tombstone)
                && Objects.equal(contentType, other.contentType)
                && Objects.equal(lastModified, other.lastModified)
                && Objects.equal(vtag, other.vtag)
                && Objects.equal(notifications, other.notifications)
                && Objects.equal(deletedIds, other.deletedIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bucket, key, vclock, tombstone, contentType,
                lastModified, vtag, notifications, deletedIds);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bucket", bucket)
                .add("key", key).add("vclock", vclock)
                .add("tombstone", tombstone).add("contentType", contentType)
                .add("lastModified", lastModified).add("vtag", vtag)
                .add("notifications", notifications)
                .add("deletedIds", deletedIds).toString();
    }
}
