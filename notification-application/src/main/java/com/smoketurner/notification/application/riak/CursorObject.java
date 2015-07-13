package com.smoketurner.notification.application.riak;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import com.basho.riak.client.api.annotations.RiakBucketName;
import com.basho.riak.client.api.annotations.RiakContentType;
import com.basho.riak.client.api.annotations.RiakKey;
import com.basho.riak.client.api.annotations.RiakLastModified;
import com.basho.riak.client.api.annotations.RiakTombstone;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.annotations.RiakVTag;
import com.basho.riak.client.api.cap.VClock;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CursorObject implements Comparable<CursorObject> {

    @RiakBucketName
    private final String bucket = "cursors";

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

    private Long value;

    /**
     * Constructor
     *
     * @param key
     * @param value
     */
    @JsonCreator
    public CursorObject(@JsonProperty("key") final String key,
            @JsonProperty("value") final Long value) {
        this.key = checkNotNull(key);
        this.value = checkNotNull(value);
    }

    public String getKey() {
        return key;
    }

    @JsonProperty
    public Long getValue() {
        return value;
    }

    @JsonProperty
    public void setValue(final long value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final CursorObject other = (CursorObject) obj;
        return Objects.equals(bucket, other.bucket)
                && Objects.equals(key, other.key)
                && Objects.equals(vclock, other.vclock)
                && Objects.equals(tombstone, other.tombstone)
                && Objects.equals(contentType, other.contentType)
                && Objects.equals(lastModified, other.lastModified)
                && Objects.equals(vtag, other.vtag)
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, key, vclock, tombstone, contentType,
                lastModified, vtag, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("key", key)
                .add("value", value).toString();
    }

    @Override
    public int compareTo(final CursorObject that) {
        return ComparisonChain.start()
                .compare(this.value, that.value, Ordering.natural().reverse())
                .result();
    }
}
