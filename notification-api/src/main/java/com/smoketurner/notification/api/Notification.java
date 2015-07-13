package com.smoketurner.notification.api;

import io.dropwizard.jackson.JsonSnakeCase;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

@Immutable
@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Notification implements Comparable<Notification> {

    private Long id;
    private String idStr;

    @NotEmpty
    private String category;

    @NotEmpty
    private String message;

    private DateTime createdAt;

    private Boolean unseen;

    private Map<String, String> properties;

    private Collection<Notification> notifications;

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
    private Notification(
            @JsonProperty("id") final Optional<Long> id,
            @JsonProperty("id_str") final Optional<String> idStr,
            @JsonProperty("category") final String category,
            @JsonProperty("message") final String message,
            @JsonProperty("created_at") final Optional<DateTime> createdAt,
            @JsonProperty("unseen") final Optional<Boolean> unseen,
            @JsonProperty("properties") final Optional<Map<String, String>> properties,
            @JsonProperty("notifications") final Optional<Collection<Notification>> notifications) {
        this.id = id.orNull();
        this.idStr = idStr.orNull();
        this.category = category;
        this.message = message;
        this.createdAt = createdAt.orNull();
        this.unseen = unseen.orNull();
        this.properties = properties.orNull();
        this.notifications = notifications.orNull();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String idStr;
        private String category;
        private String message;
        private DateTime createdAt;
        private Boolean unseen;
        private Map<String, String> properties;
        private Collection<Notification> notifications;

        public Builder fromNotification(final Notification other) {
            this.id = other.id;
            this.idStr = other.idStr;
            this.category = other.category;
            this.message = other.message;
            this.createdAt = other.createdAt;
            this.unseen = other.unseen;
            this.properties = other.properties;
            this.notifications = other.notifications;
            return this;
        }

        public Builder withId(final Long id) {
            this.id = id;
            this.idStr = String.valueOf(id);
            return this;
        }

        public Builder withCategory(final String category) {
            this.category = category;
            return this;
        }

        public Builder withMessage(final String message) {
            this.message = message;
            return this;
        }

        public Builder withCreatedAt(final DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withUnseen(final Boolean unseen) {
            this.unseen = unseen;
            return this;
        }

        public Builder withProperties(final Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder withNotifications(
                final Collection<Notification> notifications) {
            this.notifications = ImmutableList.copyOf(notifications);
            return this;
        }

        public Notification build() {
            return new Notification(Optional.fromNullable(id),
                    Optional.fromNullable(idStr), category, message,
                    Optional.fromNullable(createdAt),
                    Optional.fromNullable(unseen),
                    Optional.fromNullable(properties),
                    Optional.fromNullable(notifications));
        }
    }

    @JsonProperty
    public Optional<Long> getId() {
        return Optional.fromNullable(id);
    }

    @JsonProperty
    public Optional<String> getIdStr() {
        return Optional.fromNullable(idStr);
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
    public Optional<DateTime> getCreatedAt() {
        return Optional.fromNullable(createdAt);
    }

    @JsonProperty
    public Optional<Boolean> getUnseen() {
        return Optional.fromNullable(unseen);
    }

    @JsonProperty
    public Optional<Map<String, String>> getProperties() {
        return Optional.fromNullable(properties);
    }

    @JsonProperty
    public Optional<Collection<Notification>> getNotifications() {
        return Optional.fromNullable(notifications);
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
        return Objects.equals(id, other.id)
                && Objects.equals(idStr, other.idStr)
                && Objects.equals(category, other.category)
                && Objects.equals(message, other.message)
                && Objects.equals(createdAt, other.createdAt)
                && Objects.equals(unseen, other.unseen)
                && Objects.equals(properties, other.properties)
                && Objects.equals(notifications, other.notifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idStr, category, message, createdAt, unseen,
                properties, notifications);
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
    public int compareTo(Notification that) {
        return ComparisonChain.start()
                .compare(this.id, that.id, Ordering.natural().reverse())
                .result();
    }
}
