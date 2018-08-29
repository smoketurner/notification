/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.dropwizard.jackson.JsonSnakeCase;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.validator.constraints.NotEmpty;

@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Notification implements Comparable<Notification> {

  private final Optional<String> id;

  @NotEmpty private final String category;

  @NotEmpty private final String message;

  private final ZonedDateTime createdAt;
  private final Optional<Boolean> unseen;
  private final Map<String, String> properties;
  private final Collection<Notification> notifications;

  /**
   * Constructor
   *
   * @param category
   * @param message
   */
  @JsonCreator
  private Notification(
      @JsonProperty("category") final String category,
      @JsonProperty("message") final String message) {
    this.id = Optional.empty();
    this.category = category;
    this.message = message;
    this.createdAt = ZonedDateTime.now(Clock.systemUTC());
    this.unseen = Optional.empty();
    this.properties = Collections.emptyMap();
    this.notifications = Collections.emptyList();
  }

  /**
   * Constructor
   *
   * @param builder
   */
  private Notification(final Builder builder) {
    this.id = Optional.ofNullable(builder.id);
    this.category = builder.category;
    this.message = builder.message;
    this.createdAt =
        Optional.ofNullable(builder.createdAt).orElse(ZonedDateTime.now(Clock.systemUTC()));
    this.unseen = Optional.ofNullable(builder.unseen);
    this.properties = Optional.ofNullable(builder.properties).orElse(Collections.emptyMap());
    this.notifications = Optional.ofNullable(builder.notifications).orElse(Collections.emptyList());
  }

  /**
   * Create a new notification builder with a category and message.
   *
   * @param category Notification category
   * @param message Notification message
   * @return a notification builder
   */
  public static Builder builder(final String category, final String message) {
    return new Builder(category, message);
  }

  /**
   * Create a new notification builder with a category and an empty message.
   *
   * <p>Primarily used in the tests.
   *
   * @param category Notification category
   * @return a notification builder
   */
  public static Builder builder(final String category) {
    return builder(category, "");
  }

  /**
   * Create a new notification builder with an empty category and an empty message.
   *
   * <p>Primarily used in the tests.
   *
   * @return a notification builder
   */
  public static Builder builder() {
    return builder("", "");
  }

  /**
   * Create a new notification builder from an existing notification.
   *
   * <p>Primarily used in the tests.
   *
   * @param other Notification to copy from
   * @return a notification builder
   */
  public static Builder builder(final Notification other) {
    return builder(other.category, other.message).fromNotification(other);
  }

  /**
   * Creates a new notification with only an ID.
   *
   * <p>Primarily used in the tests.
   *
   * @param id Notification ID
   * @return Notification
   */
  public static Notification create(final String id) {
    return builder().withId(id).build();
  }

  public static class Builder {

    @Nullable private String id;

    private final String category;

    private final String message;

    @Nullable private ZonedDateTime createdAt;

    @Nullable private Boolean unseen;

    @Nullable private Map<String, String> properties;

    @Nullable private Collection<Notification> notifications;

    /**
     * Constructor
     *
     * @param category Notification category
     * @param message Notification message
     */
    public Builder(final String category, final String message) {
      this.category = Objects.requireNonNull(category);
      this.message = Objects.requireNonNull(message);
    }

    public Builder fromNotification(final Notification other) {
      this.id = other.id.orElse(null);
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

    public Builder withId(@Nullable final String id) {
      this.id = id;
      return this;
    }

    public Builder withCreatedAt(final ZonedDateTime createdAt) {
      this.createdAt = Objects.requireNonNull(createdAt, "createdAt == null");
      return this;
    }

    public Builder withUnseen(@Nullable final Boolean unseen) {
      this.unseen = unseen;
      return this;
    }

    public Builder withProperties(final Map<String, String> properties) {
      this.properties = Objects.requireNonNull(properties, "properties == null");
      return this;
    }

    public Builder withNotifications(final Collection<Notification> notifications) {
      this.notifications = Objects.requireNonNull(notifications, "notifications == null");
      return this;
    }

    public Notification build() {
      return new Notification(this);
    }
  }

  @JsonProperty
  public Optional<String> getId() {
    return id;
  }

  /**
   * This is a helper method to return the notification ID if its set, otherwise return the default
   * value given.
   *
   * <p>TODO - figure out a way to remove this method
   *
   * @param value Value to return if no notification ID is set
   * @return Notification ID or zero if not set
   */
  @JsonIgnore
  public String getId(final String value) {
    return id.orElse(value);
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
  public boolean equals(final Object obj) {
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
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("category", category)
        .add("message", message)
        .add("createdAt", createdAt)
        .add("unseen", unseen)
        .add("properties", properties)
        .add("notifications", notifications)
        .toString();
  }

  /**
   * Always sort notifications in descending order (largest IDs first)
   *
   * @param that Notification to compare to
   * @return 1 if this greater than that, 0 if this equal to that, or -1 if this less than that
   */
  @Override
  public int compareTo(final Notification that) {
    return ComparisonChain.start()
        .compare(this.getId(""), that.getId(""), Ordering.natural().reverse())
        .result();
  }
}
