/**
 * Copyright 2017 Smoke Turner, LLC.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

    private final DateTime createdAt;
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
    private Notification(@JsonProperty("id") final Long id,
            @JsonProperty("id_str") final String idStr,
            @JsonProperty("category") final String category,
            @JsonProperty("message") final String message,
            @JsonProperty("created_at") final DateTime createdAt,
            @JsonProperty("unseen") final Boolean unseen,
            @JsonProperty("properties") final Map<String, String> properties,
            @JsonProperty("notifications") final Collection<Notification> notifications) {
        this.id = Optional.ofNullable(id);
        this.idStr = Optional.ofNullable(idStr);
        this.category = category;
        this.message = message;
        this.createdAt = createdAt;
        this.unseen = Optional.ofNullable(unseen);
        this.properties = properties;
        this.notifications = notifications;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(final Notification other) {
        return builder().fromNotification(other);
    }

    public static class Builder {
        private Long id;
        private String idStr;
        private String category;
        private String message;
        private DateTime createdAt = DateTime.now(DateTimeZone.UTC);
        private Boolean unseen;
        private Map<String, String> properties = Collections.emptyMap();
        private Collection<Notification> notifications = Collections
                .emptyList();

        public Builder fromNotification(@Nonnull final Notification other) {
            this.id = other.id.orElse(null);
            this.idStr = other.idStr.orElse(null);
            this.category = other.category;
            this.message = other.message;
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

        public Builder withCategory(@Nonnull final String category) {
            this.category = Objects.requireNonNull(category);
            return this;
        }

        public Builder withMessage(@Nonnull final String message) {
            this.message = Objects.requireNonNull(message);
            return this;
        }

        public Builder withCreatedAt(@Nonnull final DateTime createdAt) {
            this.createdAt = Objects.requireNonNull(createdAt);
            return this;
        }

        public Builder withUnseen(@Nullable final Boolean unseen) {
            this.unseen = unseen;
            return this;
        }

        public Builder withProperties(
                @Nonnull final Map<String, String> properties) {
            this.properties = Objects.requireNonNull(properties);
            return this;
        }

        public Builder withNotifications(
                @Nonnull final Collection<Notification> notifications) {
            this.notifications = Objects.requireNonNull(notifications);
            return this;
        }

        public Notification build() {
            return new Notification(id, idStr, category, message, createdAt,
                    unseen, properties, notifications);
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
    public DateTime getCreatedAt() {
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
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final Notification other = (Notification) obj;
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
