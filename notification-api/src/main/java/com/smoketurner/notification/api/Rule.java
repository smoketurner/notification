/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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
import io.dropwizard.jackson.JsonSnakeCase;
import io.dropwizard.util.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import javax.annotation.Nullable;

@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Rule {

  public static final String MAX_SIZE = "max_size";
  public static final String MAX_DURATION = "max_duration";
  public static final String MATCH_ON = "match_on";

  private final Optional<Integer> maxSize;
  private final Optional<Duration> maxDuration;
  private final Optional<String> matchOn;

  /**
   * Constructor
   *
   * @param maxSize Maximum number of notifications to include in a roll-up
   * @param maxDuration Maximum time duration between the first and last notifications in a roll-up
   * @param matchOn Group notifications by a specific category
   */
  @JsonCreator
  private Rule(
      @JsonProperty(MAX_SIZE) @Nullable final Integer maxSize,
      @JsonProperty(MAX_DURATION) @Nullable final Duration maxDuration,
      @JsonProperty(MATCH_ON) @Nullable final String matchOn) {
    this.maxSize = Optional.ofNullable(maxSize);
    this.maxDuration = Optional.ofNullable(maxDuration);
    if (matchOn != null && !matchOn.isEmpty()) {
      this.matchOn = Optional.of(matchOn);
    } else {
      this.matchOn = Optional.empty();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @Nullable private Integer maxSize;

    @Nullable private Duration maxDuration;

    @Nullable private String matchOn;

    public Builder withMaxSize(@Nullable final Integer maxSize) {
      this.maxSize = maxSize;
      return this;
    }

    public Builder withMaxDuration(@Nullable final Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
    }

    public Builder withMatchOn(@Nullable final String matchOn) {
      if (matchOn != null && matchOn.isEmpty()) {
        this.matchOn = null;
      } else {
        this.matchOn = matchOn;
      }
      return this;
    }

    public Rule build() {
      return new Rule(maxSize, maxDuration, matchOn);
    }
  }

  @JsonProperty(MAX_SIZE)
  public Optional<Integer> getMaxSize() {
    return maxSize;
  }

  @JsonProperty(MAX_DURATION)
  public Optional<Duration> getMaxDuration() {
    return maxDuration;
  }

  @JsonProperty(MATCH_ON)
  public Optional<String> getMatchOn() {
    return matchOn;
  }

  @JsonIgnore
  public boolean isValid() {
    return maxSize.isPresent() || maxDuration.isPresent() || matchOn.isPresent();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final Rule other = (Rule) obj;
    return Objects.equals(maxSize, other.maxSize)
        && Objects.equals(maxDuration, other.maxDuration)
        && Objects.equals(matchOn, other.matchOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxSize, maxDuration, matchOn);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Rule.class.getSimpleName() + "{", "}")
        .add("maxSize=" + maxSize)
        .add("maxDuration=" + maxDuration)
        .add("matchOn=" + matchOn)
        .toString();
  }
}
