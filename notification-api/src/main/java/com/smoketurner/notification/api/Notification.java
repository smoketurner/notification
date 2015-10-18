/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.notification.api;

import io.dropwizard.jackson.JsonSnakeCase;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

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
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

@Immutable
@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Notification implements Comparable<Notification> {

  private final Long id;
  private final String idStr;

  @NotEmpty
  private final String category;

  @NotEmpty
  private final String message;

  private final DateTime createdAt;
  private final Boolean unseen;
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
      @JsonProperty("created_at") final Optional<DateTime> createdAt,
      @JsonProperty("unseen") final Optional<Boolean> unseen,
      @JsonProperty("properties") final Optional<Map<String, String>> properties,
      @JsonProperty("notifications") final Optional<Collection<Notification>> notifications) {
    this.id = id.orNull();
    this.idStr = idStr.orNull();
    this.category = category;
    this.message = message;
    this.createdAt = createdAt.or(DateTime.now(DateTimeZone.UTC));
    this.unseen = unseen.orNull();
    this.properties = properties.or(ImmutableMap.<String, String>of());
    this.notifications = notifications.orNull();
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
      if (id == null) {
        this.id = null;
        this.idStr = null;
      } else {
        this.id = id;
        this.idStr = String.valueOf(id);
      }
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

    public Builder withNotifications(final Collection<Notification> notifications) {
      this.notifications = ImmutableList.copyOf(notifications);
      return this;
    }

    public Notification build() {
      return new Notification(Optional.fromNullable(id), Optional.fromNullable(idStr), category,
          message, Optional.fromNullable(createdAt), Optional.fromNullable(unseen),
          Optional.fromNullable(properties), Optional.fromNullable(notifications));
    }
  }

  @JsonProperty
  public Optional<Long> getId() {
    return Optional.fromNullable(id);
  }

  /**
   * This is a helper method to return the notification ID if its set, otherwise return the default
   * value given.
   * 
   * @param value Value to return if no notification ID is set
   * @return Notification ID or zero if not set
   */
  @JsonIgnore
  public long getId(final long value) {
    if (id == null) {
      return value;
    }
    return id;
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
  public DateTime getCreatedAt() {
    return createdAt;
  }

  @JsonProperty
  public Optional<Boolean> getUnseen() {
    return Optional.fromNullable(unseen);
  }

  @JsonProperty
  public Map<String, String> getProperties() {
    return properties;
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
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("idStr", idStr)
        .add("category", category).add("message", message).add("createdAt", createdAt)
        .add("unseen", unseen).add("properties", properties).add("notifications", notifications)
        .toString();
  }

  @Override
  public int compareTo(final Notification that) {
    return ComparisonChain.start().compare(this.id, that.id, Ordering.natural().reverse()).result();
  }
}
