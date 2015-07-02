package com.smoketurner.notification.api;

import io.dropwizard.jackson.JsonSnakeCase;
import javax.annotation.concurrent.Immutable;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
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

    /**
     * Constructor
     *
     * @param id
     * @param idStr
     * @param category
     * @param message
     * @param createdAt
     */
    @JsonCreator
    private Notification(@JsonProperty("id") final Optional<Long> id,
            @JsonProperty("id_str") final Optional<String> idStr,
            @JsonProperty("category") final String category,
            @JsonProperty("message") final String message,
            @JsonProperty("created_at") final Optional<DateTime> createdAt) {
        this.id = id.orNull();
        this.idStr = idStr.orNull();
        this.category = category;
        this.message = message;
        this.createdAt = createdAt.orNull();
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

        public Builder fromNotification(final Notification other) {
            this.id = other.id;
            this.idStr = other.idStr;
            this.category = other.category;
            this.message = other.message;
            this.createdAt = other.createdAt;
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

        public Notification build() {
            return new Notification(Optional.fromNullable(id),
                    Optional.fromNullable(idStr), category, message,
                    Optional.fromNullable(createdAt));
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final Notification other = (Notification) obj;
        return Objects.equal(id, other.id) && Objects.equal(idStr, other.idStr)
                && Objects.equal(category, other.category)
                && Objects.equal(message, other.message)
                && Objects.equal(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, idStr, category, message, createdAt);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id)
                .add("idStr", idStr).add("category", category)
                .add("message", message).add("createdAt", createdAt).toString();
    }

    @Override
    public int compareTo(Notification that) {
        return ComparisonChain.start()
                .compare(this.id, that.id, Ordering.natural().reverse())
                .result();
    }
}
