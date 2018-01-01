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
package com.smoketurner.notification.api;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.hibernate.validator.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.dropwizard.jackson.JsonSnakeCase;

@Immutable
@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Notification implements Comparable<Notification> {

    private final Optional<Long> id;
    private final Optional<String> idStr;

    @NotEmpty
    private final String category;

    @NotEmpty
    private final String message;

    private final ZonedDateTime createdAt;
    private final Optional<Boolean> unseen;
    private final Map<String, String> properties;
    private final Collection<Notification> notifications;

    /**
     * Constructor
     *
     * @param id
     * @param idStr
     * @param category
     * @param message
     * @param createdAt
     * @param unseen
     * @param properties
     * @param notifications
     */
    @JsonCreator
    private Notification(@JsonProperty("id") final Optional<Long> id,
            @JsonProperty("id_str") final Optional<String> idStr,
            @JsonProperty("category") final String category,
            @JsonProperty("message") final String message,
            @JsonProperty("created_at") final Optional<ZonedDateTime> createdAt,
            @JsonProperty("unseen") final Optional<Boolean> unseen,
            @JsonProperty("properties") final Optional<Map<String, String>> properties,
            @JsonProperty("notifications") final Optional<Collection<Notification>> notifications) {
        this.id = id;
        this.idStr = idStr;
        this.category = category;
        this.message = message;
        this.createdAt = createdAt.orElse(ZonedDateTime.now(Clock.systemUTC()));
        this.unseen = unseen;
        this.properties = properties.orElse(Collections.emptyMap());
        this.notifications = notifications.orElse(Collections.emptyList());
    }

    public static Builder builder(final String category, final String message) {
        return new Builder(category, message);
    }

    public static Builder builder(final String category) {
        return builder(category, "");
    }

    public static Builder builder() {
        return builder("", "");
    }

    public static Builder builder(final Notification other) {
        return builder(other.category, other.message).fromNotification(other);
    }

    public static class Builder {

        @Nullable
        private Long id;

        @Nullable
        private String idStr;

        private final String category;
        private final String message;

        @Nullable
        private ZonedDateTime createdAt;

        @Nullable
        private Boolean unseen;

        @Nullable
        private Map<String, String> properties;

        @Nullable
        private Collection<Notification> notifications;

        /**
         * Constructor
         *
         * @param category
         *            Category
         * @param message
         *            Message
         */
        public Builder(@Nonnull final String category,
                @Nonnull final String message) {
            this.category = Objects.requireNonNull(category);
            this.message = Objects.requireNonNull(message);
        }

        public Builder fromNotification(@Nonnull final Notification other) {
            this.id = other.id.orElse(null);
            this.idStr = other.idStr.orElse(null);
            if (other.createdAt != null) {
                this.createdAt = other.createdAt;
            }
            this.unseen = other.unseen.orElse(null);
            if (other.properties != null) {
                this.properties = other.properties;
            }
            if (other.notifications != null) {
                this.notifications = other.notifications;
            }
            return this;
        }

        public Builder withId(@Nullable final Long id) {
            if (id == null) {
                this.id = null;
                this.idStr = null;
            } else {
                this.id = id;
                this.idStr = String.valueOf(id);
            }
            return this;
        }

        public Builder withCreatedAt(@Nonnull final ZonedDateTime createdAt) {
            this.createdAt = Objects.requireNonNull(createdAt,
                    "createdAt == null");
            return this;
        }

        public Builder withUnseen(@Nullable final Boolean unseen) {
            this.unseen = unseen;
            return this;
        }

        public Builder withProperties(
                @Nonnull final Map<String, String> properties) {
            this.properties = Objects.requireNonNull(properties,
                    "properties == null");
            return this;
        }

        public Builder withNotifications(
                @Nonnull final Collection<Notification> notifications) {
            this.notifications = Objects.requireNonNull(notifications,
                    "notifications == null");
            return this;
        }

        public Notification build() {
            return new Notification(Optional.ofNullable(id),
                    Optional.ofNullable(idStr), category, message,
                    Optional.ofNullable(createdAt), Optional.ofNullable(unseen),
                    Optional.ofNullable(properties),
                    Optional.ofNullable(notifications));
        }
    }

    @JsonProperty
    public Optional<Long> getId() {
        return id;
    }

    /**
     * This is a helper method to return the notification ID if its set,
     * otherwise return the default value given.
     * 
     * TODO - figure out a way to remove this method
     * 
     * @param value
     *            Value to return if no notification ID is set
     * @return Notification ID or zero if not set
     */
    @JsonIgnore
    public long getId(final long value) {
        return id.orElse(value);
    }

    @JsonProperty
    public Optional<String> getIdStr() {
        return idStr;
    }

    @JsonProperty
    public String getCategory() {
        return category;
    }

    @JsonProperty
    public String getMessage() {
        return message;
    }

    @JsonProperty
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonProperty
    public Optional<Boolean> getUnseen() {
        return unseen;
    }

    @JsonProperty
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonProperty
    public Collection<Notification> getNotifications() {
        return notifications;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final Notification other = (Notification) obj;
        // id is suppose to be globally unique, so only compare on it
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id)
                .add("idStr", idStr).add("category", category)
                .add("message", message).add("createdAt", createdAt)
                .add("unseen", unseen).add("properties", properties)
                .add("notifications", notifications).toString();
    }

    @Override
    public int compareTo(final Notification that) {
        return ComparisonChain.start().compare(this.getId(0), that.getId(0),
                Ordering.natural().reverse()).result();
    }
}
